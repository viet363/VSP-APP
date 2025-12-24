package com.example.app.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.app.Model.NotificationModel
import com.example.app.Network.NotificationApi
import com.example.app.utils.AppNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationPolling(
    private val context: Context,
    private val api: NotificationApi,
    private val userId: Long
) {

    private val handler = Handler(Looper.getMainLooper())
    private var lastId = 0L

    private val runnable = object : Runnable {
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val res = api.getNotifications(userId)
                    val list: List<NotificationModel> = res.data

                    val newNoti = list.firstOrNull { noti ->
                        noti.id > lastId && !noti.isRead
                    }

                    newNoti?.let { noti ->
                        lastId = noti.id
                        AppNotification.show(
                            context,
                            noti.title ?: "",
                            noti.message ?: ""
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            handler.postDelayed(this, 30_000)
        }
    }

    fun start() {
        handler.post(runnable)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
    }
}
