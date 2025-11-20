import React, { useEffect, useState } from 'react';
import Table from '../components/table/Table';
import { database } from '../components/Firebase/firebaseConfig';
import { ref, onValue, update } from 'firebase/database';

const customerTableHead = [
    'STT',
    'Mã đơn',
    'Tên sản phẩm',
    'Số lượng',
    'Tổng giá',
    'Tên khách hàng',
    'SĐT',
    'Địa chỉ',
    'Trạng thái'
];

const Orders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const usersRef = ref(database, 'Users');

        const unsubscribe = onValue(usersRef, (snapshot) => {
            try {
                const data = snapshot.val();
                if (data) {
                    const allOrders = [];

                    Object.entries(data).forEach(([userId, userData]) => {
                        if (userData.orders) {
                            Object.entries(userData.orders).forEach(([orderId, order]) => {
                                if (order.items && Array.isArray(order.items)) {
                                    order.items.forEach(item => {
                                        allOrders.push({
                                            id: orderId,
                                            userId: userId,
                                            productId: item.id,
                                            productTitle: item.title,
                                            quantity: item.numberInCart,
                                            total: order.total,
                                            customerName: userData.profile_name,
                                            customerPhone: userData.phone,
                                            customerAddress: userData.address,
                                            status: order.status,
                                            createdAt: order.createdAt || Date.now() 
                                        });
                                    });
                                }
                            });
                        }
                    });

                    setOrders(allOrders);

                    allOrders.forEach(order => {
                        if (!order.status || !order.createdAt) return;

                        const now = Date.now();
                        const timeDiff = now - order.createdAt; // mili giây
                        const oneDay = 24 * 60 * 60 * 1000;

                        let newStatus = order.status;

                        if (order.status === "Chờ xử lý" && timeDiff > oneDay) {
                            newStatus = "Đang giao";
                        } else if (order.status === "Đang giao" && timeDiff > 3 * oneDay) {
                            newStatus = "Đã giao";
                        }

                        if (newStatus !== order.status) {
                            const orderRef = ref(database, `Users/${order.userId}/orders/${order.id}`);
                            update(orderRef, { status: newStatus });
                        }
                    });
                }

                setLoading(false);
            } catch (err) {
                console.error("Error reading orders:", err);
                setError(err.message);
                setLoading(false);
            }
        });

        return () => unsubscribe();
    }, []);

    const handleStatusChange = (userId, orderId, newStatus) => {
        const orderRef = ref(database, `Users/${userId}/orders/${orderId}`);
        update(orderRef, { status: newStatus })
            .then(() => {
                const updatedOrders = orders.map(order =>
                    order.userId === userId && order.id === orderId
                        ? { ...order, status: newStatus }
                        : order
                );
                setOrders(updatedOrders);
            })
            .catch(error => {
                console.error("Lỗi khi cập nhật trạng thái:", error);
            });
    };

    const renderHead = (item, index) => <th key={index}>{item}</th>;

    const renderBody = (item, index) => (
        <tr key={index}>
            <td>{index + 1}</td>
            <td>{item.id || ''}</td>
            <td>{item.productTitle || 'Không có tên'}</td>
            <td>{item.quantity || 'Không có số lượng'}</td>
            <td>{item.total ? `${item.total.toLocaleString()} VND` : '0 VND'}</td>
            <td>{item.customerName || 'Không có tên'}</td>
            <td>{item.customerPhone || 'Không có SDT'}</td>
            <td>{item.customerAddress || 'Không có địa chỉ'}</td>
            <td>
                <select
                    value={item.status}
                    onChange={(e) => handleStatusChange(item.userId, item.id, e.target.value)}
                >
                    <option value="Chờ xử lý">Chờ xử lý</option>
                    <option value="Đang giao">Đang giao</option>
                    <option value="Đã giao">Đã giao</option>
                    <option value="Đã huỷ">Đã huỷ</option>
                </select>
            </td>
        </tr>
    );

    if (loading) return <div>Đang tải dữ liệu...</div>;
    if (error) return <div>Lỗi: {error}</div>;

    return (
        <div>
            <h2 className="page-header">Đơn hàng</h2>

            <div style={{ marginBottom: '16px' }}>
                <button onClick={() => window.location.reload()} className="btn btn-refresh">
                    🔄 Reset lại
                </button>
            </div>

            <div className="row">
                <div className="col-12">
                    <div className="card">
                        <div className="card__body">
                            <Table
                                limit="10"
                                headData={customerTableHead}
                                renderHead={renderHead}
                                bodyData={orders}
                                renderBody={renderBody}
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Orders;
