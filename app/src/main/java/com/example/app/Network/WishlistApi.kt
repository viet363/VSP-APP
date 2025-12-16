package com.example.app.Network

import com.example.app.Model.CommonResponse
import com.example.app.Model.WishlistResponse
import retrofit2.Call
import retrofit2.http.*

interface WishlistApi {
    @POST("mobile/wishlist/add/{productId}")
    fun addToWishlist(@Path("productId") productId: Long): Call<CommonResponse>

    @DELETE("mobile/wishlist/remove/{productId}")
    fun removeFromWishlist(@Path("productId") productId: Long): Call<CommonResponse>

    @GET("mobile/wishlist")
    fun getMyWishlist(): Call<WishlistResponse>

    @GET("mobile/wishlist/check/{productId}")
    fun checkWishlist(@Path("productId") productId: Long): Call<CommonResponse>
}