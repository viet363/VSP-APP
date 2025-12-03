package com.example.app.Network

import com.example.app.Model.UserModel
import retrofit2.Call
import retrofit2.http.*

interface AuthApi {

    @POST("auth/login")
    fun login(@Body body: HashMap<String, String>): Call<UserModel>

    @POST("auth/register")
    fun register(@Body body: HashMap<String, String>): Call<UserModel>

    @POST("auth/login-google")
    fun loginWithGoogle(@Body body: HashMap<String, String>): Call<UserModel>

    @GET("auth/profile/{id}")
    fun getProfile(@Path("id") id: Long): Call<UserModel>
}