package com.example.app.ViewModel

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


    // ------------------------- LOAD CATEGORY -------------------------
    fun loadCategories() {
        isLoading.value = true
        RetrofitClient.categoriesApi.getAllCategories()
            .enqueue(object : Callback<List<CategoryModel>> {
                override fun onResponse(
                    call: Call<List<CategoryModel>>,
                    response: Response<List<CategoryModel>>
                ) {
                    isLoading.value = false
                    categories.value = response.body() ?: emptyList()
                }

                override fun onFailure(call: Call<List<CategoryModel>>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                }
            })
    }


    // ------------------------- LOAD RECOMMENDED -------------------------
    fun loadRecommended(userId: Long) {
        isLoading.value = true
        RetrofitClient.recommendApi.getRecommendedProducts(userId.toInt())
            .enqueue(object : Callback<RecommendResponse> {
                override fun onResponse(
                    call: Call<RecommendResponse>,
                    response: Response<RecommendResponse>
                ) {
                    isLoading.value = false

                    val body = response.body()

                    if (body != null && body.success) {
                        val list = body.data?.map { ItemsModel(it) } ?: emptyList()
                        recommended.value = list

                        loadBannersFromRecommended(list)
                    } else {
                        recommended.value = emptyList()
                        banners.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<RecommendResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                }
            })
    }


    // ------------------------- LOAD FILTERED PRODUCTS -------------------------
    fun loadFiltered(categoryId: String) {
        val id = categoryId.toIntOrNull() ?: return

        RetrofitClient.productsApi.getProductsByCategory(id)
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    val list = response.body()?.data?.map { it.toItemModel() } ?: emptyList()
                    recommended.value = list

                    loadBannersFromRecommended(list)
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    errorMessage.value = t.message
                }
            })
    }


    // ------------------------- SEARCH PRODUCTS -------------------------
    fun searchProducts(query: String) {
        RetrofitClient.productsApi.searchProducts(query)
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    val list = response.body()?.data?.map { it.toItemModel() } ?: emptyList()
                    searchResults.value = list
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    errorMessage.value = t.message
                }
            })
    }


    // ------------------------- LOAD BANNERS (FROM RECOMMENDED) -------------------------
    private fun loadBannersFromRecommended(list: List<ItemsModel>) {
        if (list.isEmpty()) {
            banners.value = emptyList()
            return
        }

        val bannerList = list.mapNotNull { item ->
            if (!item.picUrl.isNullOrEmpty()) {
                SliderModel(item.picUrl.first())   // FIX: lấy ảnh đầu tiên
            } else null
        }

        banners.value = bannerList
    }
}
