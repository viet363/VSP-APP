package com.example.app.Network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentApi {
    @GET("mobile/payment/create")
    fun createVNPayUrlMobile(
        @Query("orderId") orderId: Long,
        @Query("amount") amount: Long
    ): Call<Map<String, Any>>

    @GET("mobile/payment/status/{orderId}")
    fun checkPaymentStatus(@Path("orderId") orderId: Long): Call<Map<String, Any>>
}
