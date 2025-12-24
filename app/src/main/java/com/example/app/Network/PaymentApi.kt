package com.example.app.Network

import com.example.app.Model.MoMoRequest
import com.example.app.Model.MoMoResponse
import com.example.app.Model.VNPayRequest
import com.example.app.Model.VNPayResponse
import retrofit2.Call
import retrofit2.http.*

interface PaymentApi {
    @POST("mobile/payment/create")
    fun createVNPayUrlMobile(@Body request: VNPayRequest): Call<VNPayResponse>

    @POST("mobile/payment/momo/create")
    fun createMoMoUrlMobile(@Body request: MoMoRequest): Call<MoMoResponse>

    @GET("mobile/payment/status/{orderId}")
    fun checkPaymentStatus(@Path("orderId") orderId: Long): Call<Map<String, Any>>
}