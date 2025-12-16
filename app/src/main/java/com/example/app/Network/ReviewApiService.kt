package com.example.app.Network

import com.example.app.Model.CommonResponse
import com.example.app.Model.ProductReviewModel
import retrofit2.Call
import retrofit2.http.*

interface ReviewApiService {
    @GET("mobile/reviews/product/{productId}")
    fun getProductReviews(
        @Path("productId") productId: Long
    ): Call<ApiResponse<List<ProductReviewModel>>>

    @POST("mobile/reviews")
    fun submitReview(@Body body: HashMap<String, Any>): Call<CommonResponse>

    @PUT("api/mobile/reviews/{reviewId}")
    fun updateReview(
        @Path("reviewId") reviewId: Long,
        @Body body: HashMap<String, Any>
    ): Call<CommonResponse>

    @DELETE("api/mobile/reviews/{reviewId}")
    fun deleteReview(@Path("reviewId") reviewId: Long): Call<CommonResponse>

    @GET("api/mobile/reviews/product/{productId}/stats")
    fun getRatingStats(@Path("productId") productId: Long): Call<ApiResponse<RatingStatsResponse>>
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

data class RatingStatsResponse(
    val totalReviews: Int,
    val averageRating: String,
    val ratingDistribution: Map<Int, Int>
)