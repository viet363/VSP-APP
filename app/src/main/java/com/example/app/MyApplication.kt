package com.example.app

import android.app.Application
import android.util.Log
import com.example.app.Network.RetrofitClient
import com.example.app.utils.NotificationHelper

class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication
            private set
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        RetrofitClient.init(this)
        NotificationHelper.createNotificationChannel(this)
        Log.d(TAG, "RetrofitClient initialized")
    }
}