package com.example.app.Network

import com.example.app.Model.UserModel
import com.example.app.Model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface UserApi {

    @GET("/api/mobile/user/profile")
    fun getUserProfile(): Call<UserResponse>

    @PUT("user/profile")
    fun updateUser(@Body body: HashMap<String, String>): Call<UserResponse> // Sửa lại từ UserModel -> UserResponse

    @Multipart
    @POST("user/profile/avatar")
    fun updateUserWithAvatar(
        @Part("fullname") fullname: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Call<UserResponse>

    @PUT("user/password")
    fun changePassword(@Body body: HashMap<String, String>): Call<UserResponse> // Sửa lại từ UserModel -> UserResponse
}