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

    // CÁC LIVEDATA CHO HOME SCREEN
    val categories = MutableLiveData<List<CategoryModel>>(emptyList())
    val recommended = MutableLiveData<List<ItemsModel>>(emptyList())
    val searchResults = MutableLiveData<List<ItemsModel>>(emptyList())
    val banners = MutableLiveData<List<SliderModel>>(emptyList())
    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData<Boolean>(false)

    // CÁC LIVEDATA CHO DETAIL SCREEN
    val productDetail = MutableLiveData<ProductResponse?>()
    val productSpecifications = MutableLiveData<List<ProductSpecificationModel>>(emptyList())
    val productImages = MutableLiveData<List<String>>(emptyList())
    val productReviews = MutableLiveData<List<ProductReviewModel>>(emptyList())

    // ------------------------- LOAD CATEGORY -------------------------
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

    // ------------------------- LOAD RECOMMENDED -------------------------
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

    // ------------------------- LOAD FILTERED PRODUCTS -------------------------
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

    // ------------------------- SEARCH PRODUCTS -------------------------
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

    // ------------------------- LOAD PRODUCT DETAIL -------------------------
    fun loadProductDetail(id: Int) {
        isLoading.value = true
        Log.d("MainViewModel", "Loading product detail for ID: $id")

        try {
            RetrofitClient.productsApi().getProductDetail(id)
                .enqueue(object : Callback<ProductDetailResponse> {
                    override fun onResponse(
                        call: Call<ProductDetailResponse>,
                        response: Response<ProductDetailResponse>
                    ) {
                        isLoading.value = false
                        Log.d("MainViewModel", "Product detail response code: ${response.code()}")

                        if (response.isSuccessful) {
                            val body = response.body()
                            Log.d("MainViewModel", "Success: ${body?.success}")
                            productDetail.value = body?.data
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("MainViewModel", "Error body: $errorBody")
                            errorMessage.value = "Lỗi tải chi tiết sản phẩm: ${response.code()}"
                            productDetail.value = null
                        }
                    }

                    override fun onFailure(call: Call<ProductDetailResponse>, t: Throwable) {
                        isLoading.value = false
                        Log.e("MainViewModel", "Network failure: ${t.message}")
                        errorMessage.value = "Lỗi mạng: ${t.message}"
                        productDetail.value = null
                    }
                })
        } catch (ex: Exception) {
            isLoading.value = false
            Log.e("MainViewModel", "Exception: ${ex.message}")
            errorMessage.value = ex.message
            productDetail.value = null
        }
    }

    // ------------------------- LOAD SPECIFICATIONS -------------------------
    fun loadSpecifications(id: Int) {
        isLoading.value = true
        Log.d("MainViewModel", "Loading specifications for product ID: $id")

        RetrofitClient.productsApi().getSpecifications(id)
            .enqueue(object : Callback<ProductSpecResponse> {
                override fun onResponse(
                    call: Call<ProductSpecResponse>,
                    response: Response<ProductSpecResponse>
                ) {
                    isLoading.value = false
                    Log.d("MainViewModel", "Spec response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("MainViewModel", "Spec success: ${body?.success}, count: ${body?.count}")
                        productSpecifications.value = body?.data ?: emptyList()
                    } else {
                        Log.e("MainViewModel", "Spec error: ${response.code()}")
                        productSpecifications.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductSpecResponse>, t: Throwable) {
                    isLoading.value = false
                    Log.e("MainViewModel", "Spec network failure: ${t.message}")
                    productSpecifications.value = emptyList()
                }
            })
    }

    // ------------------------- LOAD REVIEWS -------------------------
    fun loadReviews(id: Int) {
        // Tạm thời để empty list vì chưa có API
        productReviews.value = emptyList()
        Log.d("MainViewModel", "Reviews loaded (empty for now)")
    }

    // ------------------------- LOAD PRODUCT IMAGES -------------------------
    fun loadProductImages(id: Int) {
        // Lấy ảnh từ productDetail
        productDetail.observeForever { detail ->
            detail?.picUrl?.let { url ->
                if (url.isNotBlank()) {
                    productImages.value = listOf(url)
                } else {
                    productImages.value = emptyList()
                }
            }
        }
    }

    // ------------------------- CLEAR ERROR -------------------------
    fun clearError() {
        errorMessage.value = null
    }

    // ------------------------- CLEAR SEARCH -------------------------
    fun clearSearch() {
        searchResults.value = emptyList()
    }

    // ------------------------- PRIVATE FUNCTIONS -------------------------
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