package com.example.app.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.app.Model.*
import com.example.app.Network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    // LiveData
    val categories = MutableLiveData<List<CategoryModel>>(emptyList())
    val recommended = MutableLiveData<List<ItemsModel>>(emptyList())
    val searchResults = MutableLiveData<List<ItemsModel>>(emptyList())
    val productDetail = MutableLiveData<ProductResponse?>()
    val productSpecifications = MutableLiveData<List<ProductSpecificationModel>>(emptyList())
    val productImages = MutableLiveData<List<String>>(emptyList())
    val productReviews = MutableLiveData<List<ProductReviewModel>>(emptyList())
    val isLoading = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>()

    fun loadCategories() {
        isLoading.value = true
        try {
            RetrofitClient.categoriesApi.getAllCategories()
                .enqueue(object : Callback<List<CategoryModel>> { // SỬA: Thay CategoryResponse thành List<CategoryModel>
                    override fun onResponse(
                        call: Call<List<CategoryModel>>,
                        response: Response<List<CategoryModel>>
                    ) {
                        isLoading.value = false
                        if (response.isSuccessful) {
                            val body = response.body()
                            categories.value = body ?: emptyList()
                        } else {
                            errorMessage.value = "Lỗi server (categories): ${response.code()}"
                            categories.value = emptyList()
                        }
                    }

                    override fun onFailure(call: Call<List<CategoryModel>>, t: Throwable) {
                        isLoading.value = false
                        errorMessage.value = t.message
                        categories.value = emptyList()
                    }
                })
        } catch (ex: Exception) {
            isLoading.value = false
            errorMessage.value = ex.message
            categories.value = emptyList()
        }
    }

    fun loadRecommended(userId: Long) {
        isLoading.value = true
        try {
            val userIdInt = if (userId > Int.MAX_VALUE) {
                // Xử lý trường hợp userId quá lớn
                errorMessage.value = "User ID quá lớn"
                isLoading.value = false
                return
            } else {
                userId.toInt()
            }

            RetrofitClient.recommendApi.getRecommendedProducts(userIdInt)
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
                            } else {
                                recommended.value = emptyList()
                                errorMessage.value = "Không có sản phẩm đề xuất"
                            }
                        } else {
                            errorMessage.value = "Lỗi server (recommend): ${response.code()}"
                            recommended.value = emptyList()
                        }
                    }

                    override fun onFailure(call: Call<RecommendResponse>, t: Throwable) {
                        isLoading.value = false
                        errorMessage.value = t.message
                        recommended.value = emptyList()
                    }
                })
        } catch (ex: Exception) {
            isLoading.value = false
            errorMessage.value = ex.message
            recommended.value = emptyList()
        }
    }

    fun loadFiltered(categoryId: String) {
        val id = categoryId.toIntOrNull() ?: return
        isLoading.value = true
        try {
            // SỬA: Kiểm tra API endpoint tồn tại hoặc dùng API khác
            RetrofitClient.productsApi.getProductsByCategory(id) // SỬA: Thay categoriesApi bằng productsApi
                .enqueue(object : Callback<ProductListResponse> {
                    override fun onResponse(
                        call: Call<ProductListResponse>,
                        response: Response<ProductListResponse>
                    ) {
                        isLoading.value = false
                        if (response.isSuccessful) {
                            val body = response.body()
                            val list = body?.data?.map { it.toItemModel() } ?: emptyList()
                            recommended.value = list
                        } else {
                            errorMessage.value = "Lỗi tải sản phẩm theo danh mục: ${response.code()}"
                            recommended.value = emptyList()
                        }
                    }

                    override fun onFailure(call: Call<ProductListResponse>, t: Throwable) {
                        isLoading.value = false
                        errorMessage.value = t.message
                        recommended.value = emptyList()
                    }
                })
        } catch (ex: Exception) {
            isLoading.value = false
            errorMessage.value = ex.message
            recommended.value = emptyList()
        }
    }

    fun searchProducts(query: String) {
        if (query.isBlank()) {
            searchResults.value = emptyList()
            return
        }
        isLoading.value = true
        try {
            RetrofitClient.productsApi.searchProducts(query)
                .enqueue(object : Callback<ProductListResponse> {
                    override fun onResponse(
                        call: Call<ProductListResponse>,
                        response: Response<ProductListResponse>
                    ) {
                        isLoading.value = false
                        if (response.isSuccessful) {
                            val body = response.body()
                            val list = body?.data?.map { it.toItemModel() } ?: emptyList()
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
        } catch (ex: Exception) {
            isLoading.value = false
            errorMessage.value = ex.message
            searchResults.value = emptyList()
        }
    }

    fun loadProductDetail(id: Int) {
        isLoading.value = true
        try {
            RetrofitClient.productsApi.getProductDetail(id)
                .enqueue(object : Callback<ProductDetailResponse> {
                    override fun onResponse(
                        call: Call<ProductDetailResponse>,
                        response: Response<ProductDetailResponse>
                    ) {
                        isLoading.value = false
                        if (response.isSuccessful) {
                            val body = response.body()
                            productDetail.value = body?.data
                        } else {
                            errorMessage.value = "Lỗi tải chi tiết sản phẩm: ${response.code()}"
                            productDetail.value = null
                        }
                    }

                    override fun onFailure(call: Call<ProductDetailResponse>, t: Throwable) {
                        isLoading.value = false
                        errorMessage.value = t.message
                        productDetail.value = null
                    }
                })
        } catch (ex: Exception) {
            isLoading.value = false
            errorMessage.value = ex.message
            productDetail.value = null
        }
    }

    fun loadSpecifications(id: Int) {
        RetrofitClient.productsApi.getSpecifications(id)
            .enqueue(object : Callback<List<ProductSpecificationModel>> {
                override fun onResponse(
                    call: Call<List<ProductSpecificationModel>>,
                    response: Response<List<ProductSpecificationModel>>
                ) {
                    productSpecifications.value = response.body() ?: emptyList()
                }

                override fun onFailure(call: Call<List<ProductSpecificationModel>>, t: Throwable) {
                    errorMessage.value = t.message
                    productSpecifications.value = emptyList()
                }
            })
    }

    fun loadProductImages(id: Int) {
        productImages.value = emptyList()
    }

    fun loadReviews(id: Int) {
        productReviews.value = emptyList()
    }
}