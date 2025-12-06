package com.example.app.ViewModel

import android.util.Log
import androidx.lifecycle.*
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
    val isShowingFallback = MutableLiveData<Boolean>(false)

    val productDetail = MutableLiveData<ProductResponse?>()
    val productSpecifications = MutableLiveData<List<ProductSpecificationModel>>(emptyList())
    val productImages = MutableLiveData<List<String>>(emptyList())
    val productReviews = MutableLiveData<List<ProductReviewModel>>(emptyList())

    fun loadCategories() {
        isLoading.value = true

        RetrofitClient.categoriesApi().getAllCategories()
            .enqueue(object : Callback<CategoryResponse> {
                override fun onResponse(call: Call<CategoryResponse>, response: Response<CategoryResponse>) {
                    isLoading.value = false
                    if (response.isSuccessful) {
                        categories.value = response.body()?.data ?: emptyList()
                    } else {
                        sendError("Lỗi server: ${response.code()}")
                        categories.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<CategoryResponse>, t: Throwable) {
                    isLoading.value = false
                    sendError(t.message)
                    categories.value = emptyList()
                }
            })
    }

    fun loadRecommended(userId: Long) {
        Log.d("MainViewModel", "Loading recommended for user: $userId")
        isLoading.value = true
        isShowingFallback.value = false
        recommended.value = emptyList()
        searchResults.value = emptyList()

        RetrofitClient.recommendApi().getRecommendedProducts(userId.toInt())
            .enqueue(object : Callback<RecommendResponse> {
                override fun onResponse(call: Call<RecommendResponse>, response: Response<RecommendResponse>) {
                    Log.d("MainViewModel", "Recommended response code: ${response.code()}")
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("MainViewModel", "Recommended body: $body")

                        if (body != null && body.success && !body.data.isNullOrEmpty()) {
                            val list = body.data.map { ItemsModel(it) }
                            Log.d("MainViewModel", "Recommended items: ${list.size}")
                            recommended.value = list
                            loadBannersFromRecommended(list)
                        } else {
                            Log.d("MainViewModel", "Loading fallback products (recommended empty)")
                            loadFallbackProducts()
                        }
                    } else {
                        Log.d("MainViewModel", "Loading fallback products (response failed)")
                        loadFallbackProducts()
                    }
                }

                override fun onFailure(call: Call<RecommendResponse>, t: Throwable) {
                    Log.e("MainViewModel", "Recommended failed: ${t.message}")
                    isLoading.value = false
                    loadFallbackProducts()
                }
            })
    }

    private fun loadFallbackProducts() {
        Log.d("MainViewModel", "Loading fallback products - START")
        isLoading.value = true
        isShowingFallback.value = true

        try {
            RetrofitClient.productsApi().getAllProducts()
                .enqueue(object : Callback<ProductListResponse> {
                    override fun onResponse(call: Call<ProductListResponse>, response: Response<ProductListResponse>) {
                        Log.d("MainViewModel", "Fallback onResponse called - code: ${response.code()}")

                        try {
                            isLoading.value = false

                            if (response.isSuccessful) {
                                Log.d("MainViewModel", "Response is successful")
                                val body = response.body()
                                Log.d("MainViewModel", "Response body is null: ${body == null}")

                                if (body == null) {
                                    Log.e("MainViewModel", "Response body is null!")
                                    recommended.value = emptyList()
                                    return
                                }

                                // Thử parse đơn giản hơn
                                val rawBody = response.body()?.toString()
                                Log.d("MainViewModel", "Raw response (first 500 chars): ${rawBody?.take(500)}")

                                // Kiểm tra cấu trúc response
                                val hasData = !body.data.isNullOrEmpty()
                                val hasProducts = !body.products.isNullOrEmpty()
                                Log.d("MainViewModel", "Response structure - hasData: $hasData, hasProducts: $hasProducts")

                                val productList = body.getProductList()
                                Log.d("MainViewModel", "Total products from API: ${productList.size}")

                                if (productList.isNotEmpty()) {
                                    // Lấy 10 sản phẩm đầu tiên
                                    val itemsList = productList.take(10).map {
                                        try {
                                            it.toItemModel()
                                        } catch (e: Exception) {
                                            Log.e("MainViewModel", "Error converting product: ${e.message}")
                                            null
                                        }
                                    }.filterNotNull()

                                    Log.d("MainViewModel", "Successfully converted ${itemsList.size} items")

                                    if (itemsList.isNotEmpty()) {
                                        recommended.value = itemsList
                                        loadBannersFromRecommended(itemsList)
                                        Log.d("MainViewModel", "Fallback products loaded successfully - ${itemsList.size} items")
                                    } else {
                                        Log.e("MainViewModel", "No items after conversion")
                                        recommended.value = emptyList()
                                    }
                                } else {
                                    Log.e("MainViewModel", "Product list is empty")
                                    recommended.value = emptyList()
                                }

                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e("MainViewModel", "Response not successful: ${response.code()}, error: $errorBody")
                                sendError("Lỗi tải sản phẩm: ${response.code()}")
                                recommended.value = emptyList()
                            }

                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Exception in onResponse: ${e.message}")
                            e.printStackTrace()
                            isLoading.value = false
                            recommended.value = emptyList()
                        }
                    }

                    override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                        Log.e("MainViewModel", "Fallback onFailure called: ${t.message}")
                        t.printStackTrace()
                        isLoading.value = false
                        sendError(t.message ?: "Lỗi không xác định")
                        recommended.value = emptyList()
                    }
                })

        } catch (e: Exception) {
            Log.e("MainViewModel", "Exception in loadFallbackProducts: ${e.message}")
            e.printStackTrace()
            isLoading.value = false
            recommended.value = emptyList()
        }
    }

    fun loadFiltered(categoryId: String) {
        val id = categoryId.toIntOrNull() ?: return
        Log.d("MainViewModel", "Loading filtered products for category: $id")
        isLoading.value = true
        isShowingFallback.value = false
        recommended.value = emptyList()
        searchResults.value = emptyList()

        RetrofitClient.productsApi().getProductsByCategory(id)
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(call: Call<ProductListResponse>, response: Response<ProductListResponse>) {
                    Log.d("MainViewModel", "Filtered response code: ${response.code()}")
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("MainViewModel", "Filtered body structure: data=${body?.data?.size}, products=${body?.products?.size}")

                        // SỬA: Sử dụng getProductList() thay vì chỉ data
                        val products = body?.getProductList() ?: emptyList()
                        val list = products.map { it.toItemModel() }

                        Log.d("MainViewModel", "Filtered items after parse: ${list.size}")
                        recommended.value = list
                        loadBannersFromRecommended(list)
                    } else {
                        Log.e("MainViewModel", "Filtered error: ${response.errorBody()?.string()}")
                        sendError("Lỗi tải danh mục: ${response.code()}")
                        recommended.value = emptyList()
                        banners.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    Log.e("MainViewModel", "Filtered failed: ${t.message}")
                    isLoading.value = false
                    sendError(t.message)
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

        Log.d("MainViewModel", "Searching for: $query")
        isLoading.value = true
        errorMessage.value = null
        recommended.value = emptyList()

        RetrofitClient.productsApi().searchProducts(query)
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(call: Call<ProductListResponse>, response: Response<ProductListResponse>) {
                    Log.d("MainViewModel", "Search response code: ${response.code()}")
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("MainViewModel", "Search body structure: data=${body?.data?.size}, products=${body?.products?.size}")

                        // SỬA: Sử dụng getProductList() thay vì chỉ data
                        val products = body?.getProductList() ?: emptyList()
                        val results = products.map { it.toItemModel() }

                        Log.d("MainViewModel", "Search found ${results.size} items")
                        searchResults.value = results
                    } else {
                        val errorMsg = "Lỗi tìm kiếm: ${response.code()}"
                        Log.e("MainViewModel", errorMsg)
                        sendError(errorMsg)
                        searchResults.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    Log.e("MainViewModel", "Search failed: ${t.message}")
                    isLoading.value = false
                    val errorMsg = t.message ?: "Lỗi không xác định"
                    sendError(errorMsg)
                    searchResults.value = emptyList()
                }
            })
    }

    fun loadProductDetail(id: Int) {
        isLoading.value = true

        RetrofitClient.productsApi().getProductDetail(id)
            .enqueue(object : Callback<ProductDetailResponse> {
                override fun onResponse(call: Call<ProductDetailResponse>, response: Response<ProductDetailResponse>) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        productDetail.value = response.body()?.data
                        loadProductImagesDirect(response.body()?.data)
                    } else {
                        sendError("Lỗi chi tiết sản phẩm: ${response.code()}")
                        productDetail.value = null
                    }
                }

                override fun onFailure(call: Call<ProductDetailResponse>, t: Throwable) {
                    isLoading.value = false
                    sendError(t.message)
                    productDetail.value = null
                }
            })
    }

    private fun loadProductImagesDirect(detail: ProductResponse?) {
        productImages.value =
            if (!detail?.picUrl.isNullOrBlank()) listOf(detail!!.picUrl!!)
            else emptyList()
    }

    fun loadSpecifications(id: Int) {
        RetrofitClient.productsApi().getSpecification(id.toLong())
            .enqueue(object : Callback<ProductSpecResponse> {
                override fun onResponse(call: Call<ProductSpecResponse>, response: Response<ProductSpecResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        productSpecifications.value = response.body()!!.data
                    } else {
                        productSpecifications.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductSpecResponse>, t: Throwable) {
                    productSpecifications.value = emptyList()
                }
            })
    }

    fun loadReviews(id: Int) {
        productReviews.value = emptyList()
    }

    private fun loadBannersFromRecommended(list: List<ItemsModel>) {
        banners.value = list.mapNotNull { item ->
            item.picUrl.firstOrNull()?.let { SliderModel(it) }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun clearSearch() {
        searchResults.value = emptyList()
    }

    private fun sendError(msg: String?) {
        errorMessage.value = msg ?: "Lỗi không xác định"
    }

    data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String? = null
    ) {
        companion object {
            fun <T> success(data: T) = ApiResponse(true, data)
            fun <T> error(msg: String) = ApiResponse<T>(false, null, msg)
        }
    }
}