package com.example.app.Network

import com.example.app.Model.WishlistModel
import retrofit2.Call
import retrofit2.http.*

interface WishlistApi {

    @GET("wishlist/{userId}")
    fun getWishlist(@Path("userId") userId: Long): Call<List<WishlistModel>>

    @POST("wishlist/add")
    fun addWishlist(@Body body: HashMap<String, Any>): Call<WishlistModel>

    @DELETE("wishlist/delete/{id}")
    fun deleteWishlist(@Path("id") id: Long): Call<WishlistModel>
}
