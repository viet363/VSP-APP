package com.example.app.Network
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {

    @GET("products/search")
    suspend fun searchProducts(@Query("query") query: String): Response<SearchResponse>


    data class SearchResponse(
        val success: Boolean,
        val total: Int,
        val count: Int,
        val page: Int,
        val totalPages: Int,
        val query: String,
        val data: List<SearchProduct>
    )

    data class SearchProduct(
        val Id: Int,
        val Product_name: String,
        val Description: String?,
        val Price: Double,
        val picUrl: String?,
        val Category_name: String
    )
}