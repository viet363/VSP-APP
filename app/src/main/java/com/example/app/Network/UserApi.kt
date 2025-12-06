package com.example.app.Network

import com.example.app.Model.ProfileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface UserApi {

    @GET("mobile/user/profile")
    fun getUserProfile(): Call<ProfileResponse>

    @PUT("mobile/user/profile")
    fun updateUser(@Body body: Map<String, String>): Call<ProfileResponse>

    @Multipart
    @PUT("mobile/user/profile/avatar")
    fun updateUserWithAvatar(
        @Part("fullname") fullname: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Call<ProfileResponse>

    @PUT("mobile/user/password")
    fun changePassword(@Body body: Map<String, String>): Call<ProfileResponse>
}