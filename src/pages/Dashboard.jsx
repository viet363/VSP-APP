import React, { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import Chart from 'react-apexcharts';
import { useSelector } from 'react-redux';
import { ref, onValue } from 'firebase/database';
import { database } from '../components/Firebase/firebaseConfig';
import StatusCard from '../components/status-card/StatusCard';
import Table from '../components/table/Table';
import Badge from '../components/badge/Badge';

const Dashboard = () => {
    const themeReducer = useSelector(state => state.ThemeReducer.mode);

    const [totalCustomers, setTotalCustomers] = useState(0);
    const [totalProducts, setTotalProducts] = useState(0);
    const [totalOrders, setTotalOrders] = useState(0);
    const [totalRevenue, setTotalRevenue] = useState(0);
    const [topCustomers, setTopCustomers] = useState([]);
    const [latestOrders, setLatestOrders] = useState([]);
    const [monthlyRevenue, setMonthlyRevenue] = useState(Array(12).fill(0));

    useEffect(() => {
        const usersRef = ref(database, 'Users');
        const itemsRef = ref(database, 'Items');

        onValue(itemsRef, (snapshot) => {
            const data = snapshot.val();
            if (data) {
                setTotalProducts(Object.keys(data).length);
            }
        });

        onValue(usersRef, (snapshot) => {
            const data = snapshot.val();
            if (!data) return;

            const customers = [];
            const ordersList = [];
            const revenueByMonth = Array(12).fill(0);
            let totalRevenueCalc = 0;

            Object.entries(data).forEach(([userId, userData]) => {
                let totalOrders = 0;
                let totalSpend = 0;

                if (userData.orders) {
                    const orders = Object.entries(userData.orders);
                    totalOrders = orders.length;

                    orders.forEach(([orderId, order]) => {
                        const total = order.total || 0;
                        const dateStr = order.date || '';
                        totalSpend += total;
                        totalRevenueCalc += total;

                        if (dateStr) {
                            const date = new Date(dateStr);
                            const month = date.getMonth();
                            if (!isNaN(month)) {
                                revenueByMonth[month] += total;
                            }
                        }

                        ordersList.push({
                            id: orderId,
                            user: userData.profile_name || 'Không có tên',
                            total,
                            date: dateStr,
                            status: order.status || 'Chờ xử lý'
                        });
                    });
                }

                customers.push({
                    username: userData.profile_name || 'Không có tên',
                    total_orders: totalOrders,
                    total_spend: totalSpend
                });
            });

            setTotalCustomers(customers.length);
            setTotalOrders(ordersList.length);
            setTotalRevenue(totalRevenueCalc);
            setMonthlyRevenue(revenueByMonth);

            const top5 = customers
                .sort((a, b) => b.total_spend - a.total_spend)
                .slice(0, 5);
            setTopCustomers(top5);

            const latest5Orders = ordersList
                .filter(o => o.date)
                .sort((a, b) => new Date(b.date) - new Date(a.date))
                .slice(0, 5);
            setLatestOrders(latest5Orders);
        });
    }, []);

    const chartOptions = useMemo(() => ({
        chart: { background: 'transparent', toolbar: { show: false } },
        dataLabels: { enabled: false },
        stroke: { curve: 'smooth' },
        colors: ['#6ab04c'],
        xaxis: {
            categories: [
                'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
                'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
            ]
        },
        yaxis: {
            labels: {
                formatter: (value) => `${value.toLocaleString()}`
            },
            title: { text: 'Doanh thu (VND)' }
        },
        theme: { mode: themeReducer === 'theme-mode-dark' ? 'dark' : 'light' }
    }), [themeReducer]);

    const chartSeries = [
        { name: 'Doanh thu', data: monthlyRevenue }
    ];

    const orderStatus = {
        'Đang giao': 'primary',
        'Chờ xử lý': 'warning',
        'Đã giao': 'success',
        'Đã huỷ': 'danger'
    };

    const renderCustomerHead = (item, index) => <th key={index}>{item}</th>;
    const renderCustomerBody = (item, index) => (
        <tr key={index}>
            <td>{item.username}</td>
            <td>{item.total_orders}</td>
            <td>{item.total_spend.toLocaleString()} VND</td>
        </tr>
    );

    const renderOrderHead = (item, index) => <th key={index}>{item}</th>;
    const renderOrderBody = (item, index) => (
        <tr key={index}>
            <td>{item.id}</td>
            <td>{item.user}</td>
            <td>{item.total.toLocaleString()} VND</td>
            <td>{item.date}</td>
            <td><Badge type={orderStatus[item.status] || 'warning'} content={item.status} /></td>
        </tr>
    );

    const statusCards = [
        { icon: "bx bx-user", count: totalCustomers, title: "Khách hàng" },
        { icon: "bx bx-cart", count: totalOrders, title: "Đơn hàng" },
        { icon: "bx bx-box", count: totalProducts, title: "Sản phẩm" },
        { icon: "bx bx-dollar", count: `${totalRevenue.toLocaleString()} VND`, title: "Tổng doanh thu" }
    ];

    return (
        <div>
            <h2 className="page-header">Dashboard</h2>
            <p style={{ marginBottom: "20px", fontWeight: "500", fontSize: "16px" }}>
                Chào mừng bạn đến với trang quản trị của shop!
            </p>

            <div className="row">
                <div className="col-12">
                    <div className="row">
                        <div className="col-6">
                            <div className="row">
                                {statusCards.map((item, index) => (
                                    <div className="col-6" key={index}>
                                        <StatusCard icon={item.icon} count={item.count} title={item.title} />
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div className="col-6">
                            <div className="card full-height">
                                <Chart
                                    options={chartOptions}
                                    series={chartSeries}
                                    type='line'
                                    height='100%'
                                />
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-12">
                    <div className="row">
                        <div className="col-6">
                            <div className="card">
                                <div className="card__header"><h3>Khách hàng top</h3></div>
                                <div className="card__body">
                                    {topCustomers && topCustomers.length > 0 ? (
                                        <Table
                                            limit="5"
                                            headData={['Người dùng', 'Tổng đơn hàng', 'Tổng chi tiêu']}
                                            renderHead={renderCustomerHead}
                                            bodyData={topCustomers}
                                            renderBody={renderCustomerBody}
                                        />
                                    ) : (
                                        <p style={{ textAlign: 'center', padding: '20px' }}>
                                            Không có dữ liệu khách hàng.
                                        </p>
                                    )}
                                </div>
                                <div className="card__footer">
                                    <Link to='/customers'>Xem tất cả</Link>
                                </div>
                            </div>
                        </div>

                        <div className="col-6">
                            <div className="card">
                                <div className="card__header"><h3>Đơn hàng mới nhất</h3></div>
                                <div className="card__body">
                                    {latestOrders && latestOrders.length > 0 ? (
                                        <Table
                                            limit="5"
                                            headData={['Mã ĐH', 'Người dùng', 'Tổng tiền', 'Ngày', 'Trạng thái']}
                                            renderHead={renderOrderHead}
                                            bodyData={latestOrders}
                                            renderBody={renderOrderBody}
                                        />
                                    ) : (
                                        <p style={{ textAlign: 'center', padding: '20px' }}>
                                            Không có đơn hàng mới.
                                        </p>
                                    )}
                                </div>
                                <div className="card__footer">
                                    <Link to='/orders'>Xem tất cả</Link>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    );
};

export default Dashboard;
