package com.example.app.Network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PaymentApi {

    @GET("payment/create")
    fun createVNPayUrlMobile(
        @Query("orderId") orderId: Long,
        @Query("amount") amount: Long
    ): Call<Map<String, Any>>

    @GET("payment/return")
    fun vnpayReturn(
        @Query("vnp_ResponseCode") responseCode: String?,
        @Query("vnp_TxnRef") txnRef: String?
    ): Call<Map<String, Any>>
}
