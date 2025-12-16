package com.example.app.Network

import com.example.app.Model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductsApi {
    @GET("mobile/products")
    fun getAllProducts(): Call<ProductListResponse>

    @GET("mobile/products/{id}")
    fun getProductDetail(@Path("id") id: Int): Call<ProductDetailResponse>

    @GET("mobile/products/{id}/specifications")
    fun getSpecification(@Path("id") id: Long): Call<ProductSpecResponse>

    @GET("mobile/products/category/{categoryId}")
    fun getProductsByCategory(@Path("categoryId") categoryId: Int): Call<ProductListResponse>
    @GET("mobile/products/search")
    fun searchProducts(@Query("query") query: String): Call<ProductListResponse>

    @POST("mobile/products/filter")
    fun filterProducts(@Body filter: FilterRequest): Call<FilterResponse>
}