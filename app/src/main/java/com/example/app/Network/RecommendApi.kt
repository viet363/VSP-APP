package com.example.app.Network

import com.example.app.Model.RecommendResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RecommendApiService {
    @GET("mobile/recommend/{userId}")
    fun getRecommendedProducts(
        @Path("userId") userId: Int
    ): Call<RecommendResponse>
}