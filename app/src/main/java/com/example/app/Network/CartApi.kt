package com.example.app.Network

import com.example.app.Model.CartModel
import com.example.app.Model.CartDetailModel
import retrofit2.Call
import retrofit2.http.*

interface CartApi {

    @GET("cart/{userId}")
    fun getCart(@Path("userId") userId: Long): Call<CartModel>

    @POST("cart/add")
    fun addToCart(@Body body: HashMap<String, Any>): Call<CartDetailModel>

    @PUT("cart/update")
    fun updateCart(@Body body: HashMap<String, Any>): Call<CartDetailModel>

    @DELETE("cart/delete/{id}")
    fun deleteCartItem(@Path("id") id: Long): Call<CartDetailModel>
}
