package com.example.app.Network

import com.example.app.Model.ProductListResponse
import com.example.app.Model.ProductDetailResponse
import com.example.app.Model.ProductSpecResponse  // Thêm cái này
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductsApi {

    @GET("mobile/products")
    fun getAllProducts(): Call<ProductListResponse>
    @GET("mobile/products/{id}")
    fun getProductDetail(@Path("id") id: Int): Call<ProductDetailResponse>
    @GET("mobile/products/search")
    fun searchProducts(@Query("query") query: String): Call<ProductListResponse>
    @GET("mobile/products/category/{categoryId}")
    fun getProductsByCategory(@Path("categoryId") categoryId: Int): Call<ProductListResponse>
    @GET("mobile/products/specifications/{id}")
    fun getSpecifications(@Path("id") id: Int): Call<ProductSpecResponse>
}