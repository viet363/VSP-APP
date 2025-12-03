package com.example.app.Network

import com.example.app.Model.ProductListResponse
import com.example.app.Model.ProductDetailResponse
import com.example.app.Model.ProductSpecificationModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductsApi {

    @GET("products")
    fun getAllProducts(): Call<ProductListResponse>

    @GET("products/{id}")
    fun getProductDetail(@Path("id") id: Int): Call<ProductDetailResponse>

    @GET("products/search")
    fun searchProducts(@Query("query") query: String): Call<ProductListResponse>

    @GET("products/category/{categoryId}")
    fun getProductsByCategory(@Path("categoryId") categoryId: Int): Call<ProductListResponse>

    @GET("products/specifications/{id}")
    fun getSpecifications(@Path("id") id: Int): Call<List<ProductSpecificationModel>>
}
