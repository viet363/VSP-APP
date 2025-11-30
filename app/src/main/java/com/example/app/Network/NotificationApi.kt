package com.example.app.Network

import com.example.app.Model.NotificationModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface NotificationApi {

    @GET("notifications/{userId}")
    fun getNotifications(@Path("userId") userId: Long): Call<List<NotificationModel>>
}
