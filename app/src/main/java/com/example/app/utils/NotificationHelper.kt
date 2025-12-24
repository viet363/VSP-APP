package com.example.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotificationHelper {

    companion object {
        const val CHANNEL_ID = "APP_CHANNEL"
        const val CHANNEL_NAME = "Thông báo ứng dụng"
        const val CHANNEL_DESCRIPTION = "Thông báo từ ứng dụng"

        fun createNotificationChannel(context: Context) {
            // Chỉ cần cho Android 8.0 trở lên
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    importance
                ).apply {
                    description = CHANNEL_DESCRIPTION
                }

                val notificationManager = context.getSystemService(
                    NotificationManager::class.java
                )
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}