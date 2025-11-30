package com.example.app.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.app.Model.*
import com.example.app.Network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    val banners = MutableLiveData<List<SliderModel>>()

    val categories = MutableLiveData<List<CategoryModel>>()

    val recommended = MutableLiveData<List<ItemsModel>>()

    val productDetail = MutableLiveData<ProductResponse>()

    val productSpecifications = MutableLiveData<List<ProductSpecificationModel>>()

    val productImages = MutableLiveData<List<String>>()

    val productReviews = MutableLiveData<List<ProductReviewModel>>()

    val searchResults = MutableLiveData<List<ItemsModel>>(emptyList())

    val isLoading = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>()



    fun loadAllProducts() {
        isLoading.value = true
        RetrofitClient.productsApi.getAllProducts()
            .enqueue(object : Callback<List<ProductResponse>> {
                override fun onResponse(
                    call: Call<List<ProductResponse>>,
                    response: Response<List<ProductResponse>>
                ) {
                    isLoading.value = false
                    if (response.isSuccessful) {
                        val items = response.body()?.map { it.toItemModel() } ?: emptyList()
                        recommended.value = items
                    } else {
                        errorMessage.value = "Không load được sản phẩm"
                    }
                }

                override fun onFailure(call: Call<List<ProductResponse>>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = t.message
                }
            })
    }



    fun loadProductDetail(id: Int) {
        RetrofitClient.productsApi.getProductDetail(id)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(
                    call: Call<ProductResponse>,
                    response: Response<ProductResponse>
                ) {
                    if (response.isSuccessful) {
                        productDetail.value = response.body()
                    } else {
                        errorMessage.value = "Không load được chi tiết sản phẩm"
                    }
                }

                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    errorMessage.value = t.message
                }
            })
    }


    fun loadSpecifications(id: Int) {
        RetrofitClient.productsApi.getSpecifications(id.toLong())
            .enqueue(object : Callback<List<ProductSpecificationModel>> {
                override fun onResponse(
                    call: Call<List<ProductSpecificationModel>>,
                    response: Response<List<ProductSpecificationModel>>
                ) {
                    productSpecifications.value = response.body() ?: emptyList()
                }

                override fun onFailure(call: Call<List<ProductSpecificationModel>>, t: Throwable) {
                    errorMessage.value = t.message
                }
            })
    }


    fun loadReviews(id: Int) {
        productReviews.value = emptyList() // placeholder
    }


    fun loadProductImages(id: Int) {
        productImages.value = emptyList() // placeholder
    }


    fun search(query: String) {
        val source = recommended.value ?: emptyList()
        searchResults.value =
            source.filter { it.title.contains(query, ignoreCase = true) }
    }
}
