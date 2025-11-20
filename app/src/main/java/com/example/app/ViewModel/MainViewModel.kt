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

    private val _searchResults = MutableLiveData<List<ItemsModel>>(emptyList())
    val searchResults: LiveData<List<ItemsModel>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun searchProductsByName(query: String) {
        _isLoading.value = true // Bắt đầu tải
        val ref = firebaseDatabase.getReference("Items")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ItemsModel>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(ItemsModel::class.java)
                    item?.let {
                        it.id = itemSnapshot.key ?: ""
                        if (it.title.contains(query, ignoreCase = true)) {
                            items.add(it)
                        }
                    }
                }
                _searchResults.value = items // Sử dụng value vì onDataChange chạy trên luồng chính
                _isLoading.value = false // Kết thúc tải
                Log.d("MainViewModel", "Found ${items.size} products for query: $query")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainViewModel", "Search failed: ${error.message}")
                _searchResults.value = emptyList()
                _isLoading.value = false
            }
        })
    }

    fun loadFiltered(id: String) {
        val ref = firebaseDatabase.getReference("Items")
        val categoryIdLong = id.toLongOrNull() ?: return
        val query: Query = ref.orderByChild("categoryId").equalTo(categoryIdLong.toDouble())

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<ItemsModel>()
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ItemsModel::class.java)
                    if (item != null) {
                        item.id = childSnapshot.key.toString()
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
                Log.e("MainViewModel", "Load recommended failed: ${error.message}")
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
                Log.e("MainViewModel", "Load category failed: ${error.message}")
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
                Log.e("MainViewModel", "Load banners failed: ${error.message}")
            }
        })
    }
}