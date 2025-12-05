package com.example.app.ViewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.app.Model.*
import com.example.app.Network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    val categories = MutableLiveData<List<CategoryModel>>(emptyList())
    val recommended = MutableLiveData<List<ItemsModel>>(emptyList())
    val searchResults = MutableLiveData<List<ItemsModel>>(emptyList())
    val banners = MutableLiveData<List<SliderModel>>(emptyList())

    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>(false)

    fun loadCategories() {
        isLoading.value = true

        RetrofitClient.categoriesApi().getAllCategories()
            .enqueue(object : Callback<CategoryResponse> {
                override fun onResponse(
                    call: Call<CategoryResponse>,
                    response: Response<CategoryResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        // Lấy data từ CategoryResponse
                        categories.value = body?.data ?: emptyList()
                    } else {
                        errorMessage.value = "Lỗi server: ${response.code()}"
                        categories.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<CategoryResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                    categories.value = emptyList()
                }
            })
    }

    fun loadRecommended(userId: Long) {
        isLoading.value = true

        RetrofitClient.recommendApi().getRecommendedProducts(userId.toInt())
            .enqueue(object : Callback<RecommendResponse> {
                override fun onResponse(
                    call: Call<RecommendResponse>,
                    response: Response<RecommendResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body != null && body.success) {
                            val list = body.data?.map { ItemsModel(it) } ?: emptyList()
                            recommended.value = list

                            loadBannersFromRecommended(list)
                        } else {
                            recommended.value = emptyList()
                            banners.value = emptyList()
                            errorMessage.value = "Không có sản phẩm đề xuất"
                        }
                    } else {
                        errorMessage.value = "Lỗi server: ${response.code()}"
                        recommended.value = emptyList()
                        banners.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<RecommendResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                    recommended.value = emptyList()
                    banners.value = emptyList()
                }
            })
    }

    fun loadFiltered(categoryId: String) {
        val id = categoryId.toIntOrNull() ?: return
        isLoading.value = true

        RetrofitClient.productsApi().getProductsByCategory(id)
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val list = response.body()?.data?.map { it.toItemModel() } ?: emptyList()
                        recommended.value = list
                        loadBannersFromRecommended(list)
                    } else {
                        errorMessage.value = "Lỗi tải danh mục: ${response.code()}"
                        recommended.value = emptyList()
                        banners.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                    recommended.value = emptyList()
                    banners.value = emptyList()
                }
            })
    }

    fun searchProducts(query: String) {
        if (query.isBlank()) {
            searchResults.value = emptyList()
            return
        }

        isLoading.value = true
        RetrofitClient.productsApi().searchProducts(query)
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val list = response.body()?.data?.map { it.toItemModel() } ?: emptyList()
                        searchResults.value = list
                    } else {
                        errorMessage.value = "Lỗi tìm kiếm: ${response.code()}"
                        searchResults.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                    searchResults.value = emptyList()
                }
            })
    }

    private fun loadBannersFromRecommended(list: List<ItemsModel>) {
        if (list.isEmpty()) {
            banners.value = emptyList()
            return
        }

        val bannerList = list.mapNotNull { item ->
            if (!item.picUrl.isNullOrEmpty()) {
                SliderModel(item.picUrl.first())
            } else null
        }

        banners.value = bannerList
    }
}