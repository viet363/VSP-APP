package com.example.app.Network

import com.example.app.Model.BaseResponse
import com.example.app.Model.ChatResponse
import retrofit2.Call
import retrofit2.http.*

interface ChatApi {
    @GET("mobile/chat")
    fun getChat(): Call<ChatResponse>

    @POST("mobile/chat")
    fun sendMessage(@Body request: SendMessageRequest): Call<BaseResponse>

    @GET("mobile/chat/updates")
    fun getUpdates(@Query("lastId") lastId: Long): Call<ChatResponse>

}

data class SendMessageRequest(
    val adminId: Int,
    val message: String,
    val messageType: String = "text"
)