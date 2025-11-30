package com.example.app.Network

import com.example.app.Model.ProductResponse
import com.example.app.Model.ProductSpecificationModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductsApi {

    @GET("products")
    fun getAllProducts(): Call<List<ProductResponse>>
    @GET("products/{id}")
    fun getProductDetail(@Path("id") id: Int): Call<ProductResponse>
    @GET("products/{id}/specifications")
    fun getSpecifications(@Path("id") id: Long): Call<List<ProductSpecificationModel>>


}
