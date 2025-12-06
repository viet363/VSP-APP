package com.example.app.Network

import com.example.app.Model.DirectLoginResponse
import com.example.app.Model.NestedUserResponse
import com.example.app.Model.RegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    @POST("mobile/auth/login")
    fun login(@Body body: HashMap<String, String>): Call<DirectLoginResponse>

    @POST("mobile/auth/login-google")
    fun loginGoogle(@Body body: HashMap<String, String>): Call<NestedUserResponse>

    @POST("mobile/auth/register")
    fun register(@Body body: HashMap<String, String>): Call<RegisterResponse>

    @GET("mobile/auth/profile/{id}")
    fun getProfile(@Path("id") id: Long): Call<NestedUserResponse>
}