package com.example.app.Network

import com.example.app.Model.OrderRequest
import com.example.app.Model.OrderResponse
import com.example.app.Model.OrdersResponse
import retrofit2.Call
import retrofit2.http.*

interface OrdersApi {
    @POST("mobile/orders/create")
    fun createOrder(@Body request: OrderRequest): Call<OrderResponse>

    @GET("mobile/orders")
    fun getOrders(): Call<OrdersResponse>

    @GET("mobile/orders/{orderId}")
    fun getOrderDetails(@Path("orderId") orderId: Long): Call<Map<String, Any>>
}