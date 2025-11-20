import React, { useEffect, useState } from 'react';
import Table from '../components/table/Table';

const customerTableHead = ['STT','Tên','Email','SĐT','Giới tính','Ngày sinh'];

const renderHead = (item,index)=><th key={index}>{item}</th>;
const renderBody = (item,index)=>(
    <tr key={index}>
        <td>{index+1}</td>
        <td>{item.Fullname || item.Username}</td>
        <td>{item.Email || 'Không có email'}</td>
        <td>{item.Phone || 'Không có SĐT'}</td>
        <td>{item.Gender || '-'}</td>
        <td>{item.Birthday ? new Date(item.Birthday).toLocaleDateString() : '-'}</td>
    </tr>
);

const Customers = () => {
    const [customers,setCustomers] = useState([]);
    const [loading,setLoading] = useState(true);
    const [error,setError] = useState(null);

    useEffect(()=>{
        fetch('http://localhost:3001/api/customers')
            .then(res=>res.json())
            .then(data=>{
                setCustomers(data);
                setLoading(false);
            })
            .catch(err=>{
                setError(err.message);
                setLoading(false);
            });
    },[]);

    if(loading) return <div>Đang tải...</div>;
    if(error) return <div>Lỗi: {error}</div>;

    return (
        <div>
            <h2 className="page-header">Khách hàng</h2>
            <Table
                limit="10"
                headData={customerTableHead}
                renderHead={renderHead}
                bodyData={customers}
                renderBody={renderBody}
            />
        </div>
    );
};

export default Customers;
