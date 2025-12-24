package com.example.app.Network

import com.example.app.Model.NotificationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NotificationApi {

    @GET("mobile/notification")
    suspend fun getNotifications(
        @Query("userId") userId: Long
    ): NotificationResponse
}