package com.example.app.Network

import com.example.app.Model.CartResponse
import retrofit2.Call
import retrofit2.http.*

interface CartApi {

    @GET("mobile/cart")
    fun getCart(): Call<CartResponse>

    @POST("mobile/cart")
    fun addToCart(@Body body: HashMap<String, Any>): Call<Map<String, Any>>

    @PUT("mobile/cart/{id}")
    fun updateCart(
        @Path("id") id: Long,
        @Body body: HashMap<String, Any>
    ): Call<Map<String, Any>>

    @DELETE("mobile/cart/{id}")
    fun deleteCartItem(@Path("id") id: Long): Call<Map<String, Boolean>>
}
