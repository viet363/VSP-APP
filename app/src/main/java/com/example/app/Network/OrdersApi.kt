package com.example.app.Network

import com.example.app.Model.OrderModel
import com.example.app.Model.OrderDetailModel
import retrofit2.Call
import retrofit2.http.*

interface OrdersApi {

    @GET("orders")
    fun getOrders(): Call<Map<String, Any>>

    @GET("orders/{orderId}")
    fun getOrderDetail(
        @Path("orderId") orderId: Long
    ): Call<Map<String, Any>>

    @POST("orders/create")
    fun createOrder(
        @Body body: HashMap<String, Any>
    ): Call<Map<String, Any>>
}
