package com.example.app.Network

import com.example.app.Model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface UserApi {

    @GET("mobile/user/profile")
    fun getUserProfile(): Call<UserResponse>

    @PUT("mobile/user/profile")
    fun updateUser(@Body body: Map<String, String>): Call<UserResponse>

    @Multipart
    @PUT("mobile/user/profile/avatar")
    fun updateUserWithAvatar(
        @Part("fullname") fullname: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Call<UserResponse>

    @PUT("mobile/user/password")
    fun changePassword(@Body body: Map<String, String>): Call<UserResponse>
}