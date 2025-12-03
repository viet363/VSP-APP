package com.example.app.Network

import com.example.app.Model.OrderDetailResponse
import com.example.app.Model.OrderResponse // Tạo response model mới
import retrofit2.Call
import retrofit2.http.*

interface OrdersApi {

    @GET("orders")
    fun getOrders(): Call<OrderResponse>

    @GET("orders/{orderId}")
    fun getOrderDetail(
        @Path("orderId") orderId: Long
    ): Call<OrderDetailResponse>

    @POST("orders/create")
    fun createOrder(
        @Body body: HashMap<String, Any>
    ): Call<Map<String, Any>>
}