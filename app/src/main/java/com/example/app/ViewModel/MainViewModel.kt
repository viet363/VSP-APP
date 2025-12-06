package com.example.app.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.app.Model.*
import com.example.app.Network.RetrofitClient
import kotlinx.coroutines.*
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

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    fun loadRecommended(userId: Long) {
        isLoading.value = true
        recommended.value = emptyList()
        isShowingFallback.value = false

        RetrofitClient.recommendApi().getRecommendedProducts(userId.toInt())
            .enqueue(object : Callback<RecommendResponse> {
                override fun onResponse(
                    call: Call<RecommendResponse>,
                    response: Response<RecommendResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body != null && body.success && !body.data.isNullOrEmpty()) {
                            // Có sản phẩm đề xuất
                            val list = body.data.map { ItemsModel(it) }
                            recommended.value = list
                            loadBannersFromRecommended(list)
                            isShowingFallback.value = false
                            Log.d("MainViewModel", "Có ${list.size} sản phẩm đề xuất")
                        } else {
                            // Không có sản phẩm đề xuất, load sản phẩm từ danh mục
                            Log.d("MainViewModel", "Không có sản phẩm đề xuất, loading fallback products")
                            loadFallbackProducts()
                        }
                    } else {
                        errorMessage.value = "Lỗi server: ${response.code()}"
                        Log.d("MainViewModel", "API error, loading fallback products")
                        loadFallbackProducts()
                    }
                }

                override fun onFailure(call: Call<RecommendResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                    Log.d("MainViewModel", "Network failure, loading fallback products: ${t.message}")
                    loadFallbackProducts()
                }
            })
    }

    private fun loadFallbackProducts() {
        isLoading.value = true
        isShowingFallback.value = true

        // Sử dụng phương thức đơn giản - load tất cả sản phẩm rồi lấy 10 sản phẩm đầu
        RetrofitClient.productsApi().getAllProducts()
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val allProducts = response.body()?.data?.map {
                            it.toItemModel()
                        } ?: emptyList()

                        if (allProducts.isNotEmpty()) {
                            // Lấy 10 sản phẩm đầu tiên, hoặc ít hơn nếu không đủ
                            val productsToShow = allProducts.take(10)
                            recommended.value = productsToShow
                            loadBannersFromRecommended(productsToShow)
                            Log.d("MainViewModel", "Loaded ${productsToShow.size} fallback products")
                        } else {
                            recommended.value = emptyList()
                            banners.value = emptyList()
                            errorMessage.value = "Không có sản phẩm nào"
                        }
                    } else {
                        recommended.value = emptyList()
                        banners.value = emptyList()
                        errorMessage.value = "Lỗi tải sản phẩm: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    isLoading.value = false
                    recommended.value = emptyList()
                    banners.value = emptyList()
                    errorMessage.value = t.message
                }
            })
    }

    fun loadFiltered(categoryId: String) {
        val id = categoryId.toIntOrNull() ?: return
        isLoading.value = true
        isShowingFallback.value = false

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
    fun searchItems(query: String): LiveData<ApiResponse<List<ItemsModel>>> {
        val result = MutableLiveData<ApiResponse<List<ItemsModel>>>()

        // Gọi API
        RetrofitClient.productsApi().searchProducts(query)
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            val items = body.data?.map { product ->
                                ItemsModel(
                                    id = product.id.toInt(),
                                    title = product.productName,
                                    description = product.description,
                                    price = product.price.toDouble() ?: 0.0,
                                    picUrl = listOf(product.picUrl ?: ""),
                                    rating = null,
                                    score = null
                                )
                            } ?: emptyList()

                            result.value = ApiResponse.success(items)
                        } else {
                            result.value = ApiResponse.error("Search failed")
                        }
                    } else {
                        result.value = ApiResponse.error("HTTP error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    result.value = ApiResponse.error("Network error: ${t.message}")
                }
            })

        return result
    }
    fun loadReviews(id: Int) {
        productReviews.value = emptyList()
        Log.d("MainViewModel", "Reviews loaded (empty for now)")
    }

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

    fun clearError() {
        errorMessage.value = null
    }

    fun clearSearch() {
        searchResults.value = emptyList()
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
    data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String? = null
    ) {
        companion object {
            fun <T> success(data: T): ApiResponse<T> = ApiResponse(true, data)
            fun <T> error(message: String): ApiResponse<T> = ApiResponse(false, null, message)
        }
    }
}