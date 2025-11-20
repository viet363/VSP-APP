package com.example.app.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.app.Model.CategoryModel
import com.example.app.Model.ItemsModel
import com.example.app.Model.SliderModel
import com.google.firebase.database.*

class MainViewModel : ViewModel() {

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val _banner = MutableLiveData<List<SliderModel>>()
    val banners: LiveData<List<SliderModel>> = _banner

    private val _Category = MutableLiveData<MutableList<CategoryModel>>()
    val categories: LiveData<MutableList<CategoryModel>> = _Category

    private val _Recommended = MutableLiveData<MutableList<ItemsModel>>()
    val recommended: LiveData<MutableList<ItemsModel>> = _Recommended

    fun loadFiltered(id: String) {
        val ref = firebaseDatabase.getReference("Items")
        val categoryIdLong = id.toLongOrNull() ?: return // Nếu id không phải số, thoát hàm
        val query: Query = ref.orderByChild("categoryId").equalTo(categoryIdLong.toDouble()) // Firebase cần Double cho number

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<ItemsModel>()
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ItemsModel::class.java)
                    if (item != null) {
                        item?.id = childSnapshot.key.toString()
                        lists.add(item)
                    }
                }
                Log.d("MainViewModel", "Filtered items for category $id: ${lists.size}")
                _Recommended.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainViewModel", "Load filtered failed: ${error.message}")
            }
        })
    }


    fun loadRecommended() {
        val ref = firebaseDatabase.getReference("Items")
        val query: Query = ref.orderByChild("showRecommended").equalTo(true)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<ItemsModel>()
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ItemsModel::class.java)
                    if (item != null) {
                        item.id = childSnapshot.key ?: ""
                        lists.add(item)
                    }
                }
                _Recommended.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }

    fun loadCategory() {
        val ref = firebaseDatabase.getReference("Category")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<CategoryModel>()
                for (childSnapshot in snapshot.children) {
                    val category = childSnapshot.getValue(CategoryModel::class.java)
                    if (category != null) {
                        lists.add(category)
                    }
                }
                _Category.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }

    fun loadBanners() {
        val ref = firebaseDatabase.getReference("Banner")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<SliderModel>()
                for (childSnapshot in snapshot.children) {
                    val banner = childSnapshot.getValue(SliderModel::class.java)
                    if (banner != null) {
                        lists.add(banner)
                    }
                }
                _banner.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }



    // Trong MainViewModel.kt
    fun searchProducts(query: String): LiveData<List<ItemsModel>> {
        val result = MutableLiveData<List<ItemsModel>>()

        FirebaseDatabase.getInstance().getReference("Products")
            .orderByChild("title")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val products = mutableListOf<ItemsModel>()
                    for (productSnapshot in snapshot.children) {
                        productSnapshot.getValue(ItemsModel::class.java)?.let {
                            products.add(it)
                        }
                    }
                    result.value = products
                }

                override fun onCancelled(error: DatabaseError) {
                    // Xử lý lỗi
                }
            })

        return result
    }
}
