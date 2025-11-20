import React, { useEffect, useState } from 'react';
import Table from '../components/table/Table';
import { database } from '../components/Firebase/firebaseConfig';
import { ref, onValue, set, remove } from 'firebase/database';

const productTableHead = [
    'STT',
    'ID',
    'Tên',
    'Số lượng',
    'Loại',
    'Hình ảnh',
    'Mô tả',
    'Giá',
    'Đánh giá',
    'Đề xuất',
    'Hành động'
];

const Products = () => {
    const [items, setItems] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showAddForm, setShowAddForm] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [newProduct, setNewProduct] = useState({
        id: '',
        title: '',
        model: [],
        picUrl: [],
        description: '',
        price: '',
        rating: '',
        quantity: 1,
        categoryId: '',
        showRecommended: false
    });
    const [tempModel, setTempModel] = useState('');
    const [tempPicUrl, setTempPicUrl] = useState('');

    const renderHead = (item, index) => <th key={index}>{item}</th>;

    const renderBody = (item, index) => (
        <tr key={index}>
            <td>{index + 1}</td>
            <td>{item.id}</td>
            <td>{item.title}</td>
            <td>{item.quantity || 0}</td>
            <td>{item.model?.join(', ')}</td>
            <td>
                {item.picUrl?.map((url, idx) => (
                    <img key={idx} src={url} alt="item" style={{ width: '50px', marginRight: '5px' }} />
                ))}
            </td>
            <td>{item.description}</td>
            <td>{item.price}</td>
            <td>{item.rating}</td>
            <td>{item.showRecommended ? 'Có' : 'Không'}</td>
            <td>
                <button className="btn-edit" onClick={() => handleEditProduct(item)}>Sửa</button>
                <button className="btn-delete" onClick={() => handleDeleteProduct(item.id)}>Xóa</button>
            </td>
        </tr>
    );

    const handleEditProduct = (product) => {
        setNewProduct(product);
        setShowAddForm(true);
        setEditMode(true);
    };

    const handleDeleteProduct = (productId) => {
        if (window.confirm('Bạn có chắc chắn muốn xóa sản phẩm này không?')) {
            const productRef = ref(database, `Items/${productId}`);
            remove(productRef)
                .then(() => {
                    alert('Xóa sản phẩm thành công!');
                })
                .catch(error => {
                    console.error("Error deleting product: ", error);
                    alert('Có lỗi xảy ra khi xóa sản phẩm');
                });
        }
    };

    const handleAddProduct = () => {
        const maxId = items.reduce((max, item) => {
            const itemId = parseInt(item.id);
            return itemId > max ? itemId : max;
        }, 0);
        
        setNewProduct({
            id: (maxId + 1).toString(),
            title: '',
            model: [],
            picUrl: [],
            description: '',
            price: '',
            rating: '',
            quantity: 1,
            categoryId: '',
            showRecommended: false
        });
        setShowAddForm(true);
        setEditMode(false);
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setNewProduct({
            ...newProduct,
            [name]: value
        });
    };

    const handleCheckboxChange = (e) => {
        const { name, checked } = e.target;
        setNewProduct({
            ...newProduct,
            [name]: checked
        });
    };

    const addModel = () => {
        if (tempModel.trim()) {
            setNewProduct({
                ...newProduct,
                model: [...newProduct.model, tempModel]
            });
            setTempModel('');
        }
    };

    const addPicUrl = () => {
        if (tempPicUrl.trim()) {
            setNewProduct({
                ...newProduct,
                picUrl: [...newProduct.picUrl, tempPicUrl]
            });
            setTempPicUrl('');
        }
    };

    const removeModel = (index) => {
        const updatedModels = newProduct.model.filter((_, i) => i !== index);
        setNewProduct({
            ...newProduct,
            model: updatedModels
        });
    };

    const removePicUrl = (index) => {
        const updatedPicUrls = newProduct.picUrl.filter((_, i) => i !== index);
        setNewProduct({
            ...newProduct,
            picUrl: updatedPicUrls
        });
    };

    const submitProduct = (e) => {
        e.preventDefault();
        
        if (!newProduct.title || !newProduct.price || !newProduct.categoryId) {
            alert('Vui lòng điền đầy đủ thông tin bắt buộc (Tên, Giá, Danh mục)');
            return;
        }

        const productToSubmit = {
            ...newProduct,
            price: parseFloat(newProduct.price),
            rating: newProduct.rating ? parseFloat(newProduct.rating) : 0,
            quantity: parseInt(newProduct.quantity) || 1,
            categoryId: parseInt(newProduct.categoryId)
        };

        set(ref(database, `Items/${newProduct.id}`), productToSubmit)
            .then(() => {
                alert(editMode ? 'Cập nhật sản phẩm thành công!' : 'Thêm sản phẩm thành công!');
                setNewProduct({
                    id: '',
                    title: '',
                    model: [],
                    picUrl: [],
                    description: '',
                    price: '',
                    rating: '',
                    quantity: 1,
                    categoryId: '',
                    showRecommended: false
                });
                setShowAddForm(false);
                setEditMode(false);
            })
            .catch(error => {
                console.error("Error saving product: ", error);
                alert('Có lỗi xảy ra khi lưu sản phẩm');
            });
    };

    useEffect(() => {
        const itemsRef = ref(database, 'Items');
        const categoriesRef = ref(database, 'Category');
        
        const unsubscribeItems = onValue(itemsRef, (snapshot) => {
            try {
                const data = snapshot.val();
                if (data) {
                    const itemList = Object.entries(data)
                        .map(([key, value]) => ({
                            id: key,
                            ...value
                        }))
                        .sort((a, b) => parseInt(a.id) - parseInt(b.id));
                    
                    setItems(itemList);
                }
                setLoading(false);
            } catch (err) {
                setError(err.message);
                setLoading(false);
            }
        });

        const unsubscribeCategories = onValue(categoriesRef, (snapshot) => {
            const data = snapshot.val();
            if (data) setCategories(data);
        });

        return () => {
            unsubscribeItems();
            unsubscribeCategories();
        };
    }, []);

    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;

    return (
        <div>
            <div className="page-header">
                <h2>Products</h2>
                <button className="btn btn-add_product" onClick={handleAddProduct}>
                    + Thêm sản phẩm
                </button>
            </div>

            {showAddForm && (
                <div className="add-product-form">
                    <h3>{editMode ? 'Sửa sản phẩm' : 'Thêm sản phẩm mới'}</h3>
                    <form onSubmit={submitProduct}>
                        <div className="form-group">
                            <label>ID sản phẩm:</label>
                            <input type="text" name="id" value={newProduct.id} onChange={handleInputChange} disabled />
                        </div>

                        <div className="form-group">
                            <label>Tên sản phẩm:</label>
                            <input type="text" name="title" value={newProduct.title} onChange={handleInputChange} required />
                        </div>

                        <div className="form-group">
                            <label>Số lượng:</label>
                            <input type="number" name="quantity" value={newProduct.quantity} onChange={handleInputChange} required min="1" />
                        </div>

                        <div className="form-group">
                            <label>Danh mục:</label>
                            <select name="categoryId" value={newProduct.categoryId} onChange={handleInputChange} required>
                                <option value="">-- Chọn danh mục --</option>
                                {Object.entries(categories).map(([key, category]) => (
                                    <option key={key} value={category.id}>{category.title}</option>
                                ))}
                            </select>
                        </div>

                        <div className="form-group">
                            <label>Loại sản phẩm:</label>
                            <div className="input-group">
                                <input type="text" value={tempModel} onChange={(e) => setTempModel(e.target.value)} />
                                <button type="button" onClick={addModel}>Thêm</button>
                            </div>
                            <div className="tag-container">
                                {newProduct.model.map((model, index) => (
                                    <span key={index} className="tag">
                                        {model}
                                        <button type="button" onClick={() => removeModel(index)}>×</button>
                                    </span>
                                ))}
                            </div>
                        </div>

                        <div className="form-group">
                            <label>Hình ảnh (URL):</label>
                            <div className="input-group">
                                <input type="text" value={tempPicUrl} onChange={(e) => setTempPicUrl(e.target.value)} />
                                <button type="button" onClick={addPicUrl}>Thêm</button>
                            </div>
                            <div className="tag-container">
                                {newProduct.picUrl.map((url, index) => (
                                    <span key={index} className="tag">
                                        <img src={url} alt="preview" style={{ width: '30px', height: '30px' }} />
                                        <button type="button" onClick={() => removePicUrl(index)}>×</button>
                                    </span>
                                ))}
                            </div>
                        </div>

                        <div className="form-group">
                            <label>Mô tả:</label>
                            <textarea name="description" value={newProduct.description} onChange={handleInputChange} />
                        </div>

                        <div className="form-group">
                            <label>Giá:</label>
                            <input type="number" name="price" value={newProduct.price} onChange={handleInputChange} required min="0" />
                        </div>

                        <div className="form-group">
                            <label>Đánh giá (1-5):</label>
                            <input type="number" name="rating" value={newProduct.rating} onChange={handleInputChange} min="1" max="5" step="0.1" />
                        </div>

                        <div className="form-group checkbox-group">
                            <label>
                                <input type="checkbox" name="showRecommended" checked={newProduct.showRecommended} onChange={handleCheckboxChange} />
                                Hiển thị ở mục đề xuất
                            </label>
                        </div>

                        <div className="form-actions">
                            <button type="submit" className="btn-submit">Lưu</button>
                            <button type="button" className="btn-cancel" onClick={() => setShowAddForm(false)}>Hủy</button>
                        </div>
                    </form>
                </div>
            )}

            <div className="card">
                <div className="card__body">
                    <Table
                        limit="10"
                        headData={productTableHead}
                        renderHead={renderHead}
                        bodyData={items}
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

export default Products;
