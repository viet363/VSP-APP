package com.example.app.Network

import com.example.app.Model.OrderModel
import retrofit2.Call
import retrofit2.http.*

interface OrdersApi {
    @GET("orders")
    fun getOrders(): Call<List<OrderModel>>

    @GET("orders/{orderId}")
    fun getOrderDetail(
        @Path("orderId") orderId: Long
    ): Call<OrderModel>
}