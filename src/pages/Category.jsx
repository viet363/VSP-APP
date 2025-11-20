import React, { useEffect, useState } from 'react';
import Table from '../components/table/Table';
import { database } from '../components/Firebase/firebaseConfig';
import { ref, onValue, set, remove } from 'firebase/database';

const categoryTableHead = [
    'STT',
    'ID',
    'Tên danh mục',
    'Hình ảnh',
    'Hành động'
];

const Categories = () => {
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);
    const [category, setCategory] = useState({ id: '', title: '', picUrl: '' });

    useEffect(() => {
        const categoriesRef = ref(database, 'Category');
        const unsubscribe = onValue(categoriesRef, (snapshot) => {
            try {
                const data = snapshot.val();
                if (data) {
                    const list = Object.entries(data)
                        .map(([key, value]) => ({ id: key, ...value }))
                        .sort((a, b) => parseInt(a.id) - parseInt(b.id));
                    setCategories(list);
                }
                setLoading(false);
            } catch (err) {
                setError(err.message);
                setLoading(false);
            }
        });
        return () => unsubscribe();
    }, []);

    const handleAdd = () => {
        const maxId = categories.reduce((max, c) => Math.max(max, parseInt(c.id)), 0);
        setCategory({ id: (maxId + 1).toString(), title: '', picUrl: '' });
        setEditMode(false);
        setShowForm(true);
    };

    const handleEdit = (item) => {
        setCategory(item);
        setCurrentId(item.id);
        setEditMode(true);
        setShowForm(true);
    };

    const handleDelete = (id) => {
        if (window.confirm('Bạn có chắc muốn xóa danh mục này?')) {
            remove(ref(database, `Category/${id}`))
                .then(() => alert('Xóa thành công!'))
                .catch((e) => alert('Lỗi khi xóa: ' + e.message));
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setCategory({ ...category, [name]: value });
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!category.title || !category.picUrl) {
            alert('Vui lòng nhập đầy đủ thông tin.');
            return;
        }
        set(ref(database, `Category/${category.id}`), category)
            .then(() => {
                alert(editMode ? 'Cập nhật danh mục thành công!' : 'Thêm danh mục thành công!');
                setShowForm(false);
            })
            .catch((e) => alert('Lỗi khi lưu: ' + e.message));
    };

    const renderHead = (item, index) => <th key={index}>{item}</th>;
    const renderBody = (item, index) => (
        <tr key={index}>
            <td>{index + 1}</td>
            <td>{item.id}</td>
            <td>{item.title}</td>
            <td><img src={item.picUrl} alt="category" style={{ width: 50, height: 50 }} /></td>
            <td>
                <button className="btn-edit" onClick={() => handleEdit(item)}>Sửa</button>
                <button className="btn-delete" onClick={() => handleDelete(item.id)}>Xóa</button>
            </td>
        </tr>
    );

    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;

    return (
        <div>
            <div className="page-header">
                <h2>Danh mục sản phẩm</h2>
                <button className="btn btn-add" onClick={handleAdd}>+ Thêm danh mục</button>
            </div>

            {showForm && (
                <div className="add-form">
                    <h3>{editMode ? 'Sửa danh mục' : 'Thêm danh mục mới'}</h3>
                    <form onSubmit={handleSubmit}>
                        <label>ID:</label>
                        <input type="text" name="id" value={category.id} disabled />

                        <label>Tên danh mục:</label>
                        <input type="text" name="title" value={category.title} onChange={handleChange} required />

                        <label>URL hình ảnh:</label>
                        <input type="text" name="picUrl" value={category.picUrl} onChange={handleChange} required />

                        {category.picUrl && <img src={category.picUrl} alt="preview" style={{ width: 100, marginTop: 10 }} />}

                        <div className="form-actions">
                            <button type="submit" className="btn-submit">Lưu</button>
                            <button type="button" onClick={() => setShowForm(false)} className="btn-cancel">Hủy</button>
                        </div>
                    </form>
                </div>
            )}

            <div className="card">
                <div className="card__body">
                    <Table
                        limit="10"
                        headData={categoryTableHead}
                        renderHead={renderHead}
                        bodyData={categories}
                        renderBody={renderBody}
                    />
                </div>
            </div>

            <style jsx>{`
                .btn-edit {
                    background: #2196f3;
                    color: white;
                    padding: 5px 10px;
                    border: none;
                    border-radius: 4px;
                    cursor: pointer;
                    margin-right: 5px;
                }
                .btn-edit:hover { background: #1976d2; }
                .btn-delete {
                    background: #f44336;
                    color: white;
                    padding: 5px 10px;
                    border: none;
                    border-radius: 4px;
                    cursor: pointer;
                }
                .btn-delete:hover { background: #d32f2f; }
            `}</style>
        </div>
    );
};

export default Categories;
