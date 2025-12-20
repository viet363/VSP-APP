package com.example.app.ViewModel

import android.util.Log
import androidx.lifecycle.*
import com.example.app.Model.*
import com.example.app.Network.ApiResponse
import com.example.app.Network.RetrofitClient
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    private val _productRating = MutableLiveData<Float>()
    val productRating: LiveData<Float> get() = _productRating

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
        Log.d("MainViewModel", "Loading recommended with fallback for user: $userId")
        isLoading.value = true
        isShowingFallback.value = false
        recommended.value = emptyList()
        searchResults.value = emptyList()

        RetrofitClient.recommendApi().getRecommendedProducts(userId.toInt())
            .enqueue(object : Callback<RecommendResponse> {
                override fun onResponse(
                    call: Call<RecommendResponse>,
                    response: Response<RecommendResponse>
                ) {
                    Log.d("MainViewModel", "Recommended response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val body = response.body()
                        val recommendedCount = body?.data?.size ?: 0
                        Log.d("MainViewModel", "Recommended items count: $recommendedCount")

                        if (body != null && body.success && !body.data.isNullOrEmpty()) {
                            val recommendedList = body.data.map { recommendedProduct ->
                                ItemsModel(
                                    id = recommendedProduct.Id,
                                    title = recommendedProduct.Product_name,
                                    description = recommendedProduct.Description,
                                    price = recommendedProduct.Price,
                                    picUrlString = recommendedProduct.picUrl,
                                    rating = recommendedProduct.Score?.toDouble(),
                                    score = recommendedProduct.Score,
                                    numberInCart = 1,
                                    isRecommended = true
                                )
                            }

                            val minItems = 100

                            if (recommendedList.size >= minItems) {
                                Log.d(
                                    "MainViewModel",
                                    "Đủ sản phẩm đề xuất: ${recommendedList.size}"
                                )
                                recommended.value = recommendedList.take(minItems)
                                loadBannersFromRecommended(recommendedList)
                                isLoading.value = false
                                isShowingFallback.value = false
                            } else {
                                Log.d(
                                    "MainViewModel",
                                    "Chỉ có $recommendedCount sản phẩm đề xuất. Đang bổ sung..."
                                )
                                loadAdditionalProducts(recommendedList, minItems)
                            }
                        } else {
                            Log.d("MainViewModel", "Không có sản phẩm đề xuất. Loading fallback...")
                            loadFallbackProducts(20)
                        }
                    } else {
                        Log.d("MainViewModel", "Recommend API error. Loading fallback...")
                        loadFallbackProducts(20)
                    }
                }

                override fun onFailure(call: Call<RecommendResponse>, t: Throwable) {
                    Log.e("MainViewModel", "Recommend API failed: ${t.message}")
                    loadFallbackProducts(20)
                }
            })
    }

    private fun loadAdditionalProducts(recommendedList: List<ItemsModel>, minItems: Int) {
        Log.d("MainViewModel", "Loading additional products...")

        RetrofitClient.productsApi().getAllProducts()
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        val allProducts = body?.getProductList() ?: emptyList()

                        if (allProducts.isNotEmpty()) {
                            val additionalItems = allProducts.map {
                                it.toItemModel().copy(isRecommended = false)
                            }

                            val recommendedIds = recommendedList.map { it.id }.toSet()
                            val uniqueAdditional = additionalItems
                                .filterNot { recommendedIds.contains(it.id) }
                                .take(minItems - recommendedList.size)

                            // Kết hợp: Đề xuất trước, bổ sung sau
                            val combinedList = recommendedList + uniqueAdditional

                            Log.d(
                                "MainViewModel", """
                            Kết hợp thành công:
                               - Đề xuất: ${recommendedList.size} sản phẩm
                               - Bổ sung: ${uniqueAdditional.size} sản phẩm
                               - Tổng: ${combinedList.size} sản phẩm
                            """.trimIndent()
                            )

                            recommended.value = combinedList
                            loadBannersFromRecommended(combinedList)
                            isShowingFallback.value = true
                        } else {
                            // Không có sản phẩm bổ sung
                            Log.d("MainViewModel", "Không có sản phẩm bổ sung")
                            recommended.value = recommendedList
                            loadBannersFromRecommended(recommendedList)
                            isShowingFallback.value = false
                        }
                    } else {
                        Log.e("MainViewModel", "Không thể load sản phẩm bổ sung")
                        // Vẫn hiển thị sản phẩm đề xuất đã có
                        recommended.value = recommendedList
                        loadBannersFromRecommended(recommendedList)
                        isShowingFallback.value = false
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    isLoading.value = false
                    Log.e("MainViewModel", "Load additional products failed: ${t.message}")
                    // Vẫn hiển thị sản phẩm đề xuất đã có
                    recommended.value = recommendedList
                    loadBannersFromRecommended(recommendedList)
                    isShowingFallback.value = false
                }
            })
    }

    // Hàm load fallback khi không có đề xuất
    fun loadFallbackProducts(minItems: Int = 20) {
        Log.d("MainViewModel", "Loading fallback products (min: $minItems)")
        isLoading.value = true
        isShowingFallback.value = true

        RetrofitClient.productsApi().getAllProducts()
            .enqueue(object : Callback<ProductListResponse> {
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        val allProducts = body?.getProductList() ?: emptyList()

                        if (allProducts.isNotEmpty()) {
                            // Lấy sản phẩm phổ biến (không phải đề xuất)
                            val itemsList = allProducts.take(minItems).map {
                                it.toItemModel().copy(isRecommended = false)
                            }

                            Log.d("MainViewModel", "Fallback loaded: ${itemsList.size} products")
                            recommended.value = itemsList
                            loadBannersFromRecommended(itemsList)
                        } else {
                            Log.e("MainViewModel", "Không có sản phẩm nào")
                            recommended.value = emptyList()
                        }
                    } else {
                        Log.e("MainViewModel", "Fallback response error")
                        sendError("Lỗi tải sản phẩm: ${response.code()}")
                        recommended.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    isLoading.value = false
                    Log.e("MainViewModel", "Fallback failed: ${t.message}")
                    sendError(t.message ?: "Lỗi không xác định")
                    recommended.value = emptyList()
                }
            })
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
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    Log.d("MainViewModel", "Filtered response code: ${response.code()}")
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        val products = body?.getProductList() ?: emptyList()
                        val list = products.take(20).map {
                            it.toItemModel().copy(isRecommended = false)
                        }

                        Log.d("MainViewModel", "Filtered items: ${list.size}")
                        recommended.value = list
                        loadBannersFromRecommended(list)
                    } else {
                        Log.e("MainViewModel", "Filtered error: ${response.code()}")
                        sendError("Lỗi tải danh mục: ${response.code()}")
                        recommended.value = emptyList()
                        banners.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                    isLoading.value = false
                    Log.e("MainViewModel", "Filtered failed: ${t.message}")
                    sendError(t.message)
                    recommended.value = emptyList()
                    banners.value = emptyList()
                }
            })
    }

    // Hàm search (giữ nguyên)
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
                override fun onResponse(
                    call: Call<ProductListResponse>,
                    response: Response<ProductListResponse>
                ) {
                    Log.d("MainViewModel", "Search response code: ${response.code()}")
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        val products = body?.getProductList() ?: emptyList()
                        val results = products.map { it.toItemModel().copy(isRecommended = false) }

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

    // Các hàm khác giữ nguyên...
    fun loadProductDetail(id: Int) {
        isLoading.value = true

        RetrofitClient.productsApi().getProductDetail(id)
            .enqueue(object : Callback<ProductDetailResponse> {
                override fun onResponse(
                    call: Call<ProductDetailResponse>,
                    response: Response<ProductDetailResponse>
                ) {
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
                override fun onResponse(
                    call: Call<ProductSpecResponse>,
                    response: Response<ProductSpecResponse>
                ) {
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

    fun loadReviews(productId: Int) {
        isLoading.value = true
        RetrofitClient.reviewApi().getProductReviews(productId.toLong())
            .enqueue(object : Callback<ApiResponse<List<ProductReviewModel>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<ProductReviewModel>>>,
                    response: Response<ApiResponse<List<ProductReviewModel>>>
                ) {
                    isLoading.value = false

                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        Log.d("MainViewModel", "Reviews API response: ${apiResponse?.success}")
                        Log.d("MainViewModel", "Reviews data: ${apiResponse?.data?.size}")

                        if (apiResponse?.success == true) {
                            val reviews = apiResponse.data ?: emptyList()
                            productReviews.value = reviews

                            // Tính toán và cập nhật rating trung bình
                            if (reviews.isNotEmpty()) {
                                val averageRating = reviews.map { it.rating }.average().toFloat()
                                _productRating.value = averageRating
                                Log.d("MainViewModel", "Average rating calculated: $averageRating")
                            } else {
                                _productRating.value = 0f
                            }
                        } else {
                            productReviews.value = emptyList()
                            _productRating.value = 0f
                            Log.e("MainViewModel", "API error: ${apiResponse?.message}")
                            sendError(apiResponse?.message ?: "Lỗi tải đánh giá")
                        }
                    } else {
                        productReviews.value = emptyList()
                        _productRating.value = 0f
                        Log.e("MainViewModel", "Response error: ${response.code()}")
                        sendError("Lỗi tải đánh giá: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<List<ProductReviewModel>>>,
                    t: Throwable
                ) {
                    isLoading.value = false
                    productReviews.value = emptyList()
                    _productRating.value = 0f
                    Log.e("MainViewModel", "Network error: ${t.message}")
                    sendError("Lỗi kết nối: ${t.message}")
                }
            })
    }

    // Hàm cập nhật rating sau khi submit review thành công
    fun updateRatingAfterReview(newRating: Int, currentReviewCount: Int) {
        val currentRating = _productRating.value ?: 0f
        val newAverage = if (currentReviewCount > 0) {
            (currentRating * currentReviewCount + newRating) / (currentReviewCount + 1)
        } else {
            newRating.toFloat()
        }
        _productRating.value = newAverage
        Log.d("MainViewModel", "Rating updated to: $newAverage (new review: $newRating, count: ${currentReviewCount + 1})")
    }

    private fun loadBannersFromRecommended(list: List<ItemsModel>) {
        banners.value = list.mapNotNull { item ->
            item.picUrl.firstOrNull()?.let { SliderModel(it) }
        }.take(5) // Chỉ lấy 5 banner đầu tiên
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

    override fun onCleared() {
        super.onCleared()
        coroutineScope.cancel()
    }

    fun filterProducts(filterRequest: FilterRequest) {
        Log.d("MainViewModel", "=== FILTER PRODUCTS CALLED ===")
        Log.d("MainViewModel", "Filter request: $filterRequest")

        isLoading.value = true
        errorMessage.value = null

        recommended.value = emptyList()
        searchResults.value = emptyList()

        RetrofitClient.productsApi().filterProducts(filterRequest)
            .enqueue(object : Callback<FilterApiResponse> {
                override fun onResponse(
                    call: Call<FilterApiResponse>,
                    response: Response<FilterApiResponse>
                ) {
                    isLoading.value = false
                    Log.d("MainViewModel", "Filter response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d("MainViewModel", "Filter response success: ${result?.success}")
                        Log.d("MainViewModel", "Filter data count: ${result?.data?.size ?: 0}")

                        // Debug chi tiết
                        result?.data?.forEachIndexed { index, item ->
                            Log.d("MainViewModel",
                                "Item $index: id=${item.id}, title=${item.productName?.take(20)}, " +
                                        "price=${item.price}, picUrl length=${item.picUrl?.length ?: 0}"
                            )
                        }

                        if (result?.success == true && !result.data.isNullOrEmpty()) {
                            // Convert FilterProduct sang ItemsModel
                            val filteredItems = result.data.mapNotNull { filterItem ->
                                try {
                                    filterItem.toItemsModel()
                                } catch (e: Exception) {
                                    Log.e("MainViewModel", "Error converting item ${filterItem.id}: ${e.message}")
                                    null
                                }
                            }

                            Log.d("MainViewModel", "Filter successful: ${filteredItems.size} products found")

                            if (filteredItems.isNotEmpty()) {
                                recommended.value = filteredItems
                                loadBannersFromRecommended(filteredItems)
                            } else {
                                sendError("Không thể parse dữ liệu sản phẩm")
                                recommended.value = emptyList()
                            }
                        } else {
                            val message = result?.message ?: "Không có sản phẩm phù hợp"
                            Log.w("MainViewModel", "Filter empty: $message")
                            sendError(message)
                            recommended.value = emptyList()
                        }
                    } else {
                        try {
                            val errorBody = response.errorBody()?.string()
                            Log.e("MainViewModel", "Filter error ${response.code()}: $errorBody")
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Cannot read error body: ${e.message}")
                        }

                        sendError("Lỗi lọc sản phẩm: ${response.code()}")
                        recommended.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<FilterApiResponse>, t: Throwable) {
                    isLoading.value = false
                    Log.e("MainViewModel", "Filter failed: ${t.message}")
                    t.printStackTrace()
                    sendError("Lỗi kết nối: ${t.message}")
                    recommended.value = emptyList()
                }
            })
    }
}