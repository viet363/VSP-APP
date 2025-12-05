package com.example.app

import android.app.Application
import android.util.Log
import com.example.app.Network.RetrofitClient

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        RetrofitClient.init(this)
        Log.d(TAG, "RetrofitClient initialized")
    }
}