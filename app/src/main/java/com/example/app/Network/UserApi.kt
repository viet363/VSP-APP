package com.example.app.Network

import com.example.app.Model.UserModel
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface UserApi {

    @GET("user/profile")
    fun getUserProfile(): Call<UserModel>

    @PUT("user/profile")
    fun updateUser(@Body body: HashMap<String, Any>): Call<UserModel>

    @Multipart
    @POST("user/profile/avatar")
    fun updateUserWithAvatar(
        @Part("fullname") fullname: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part avatar: MultipartBody.Part
    ): Call<UserModel>

    @PUT("user/password")
    fun changePassword(@Body body: HashMap<String, String>): Call<UserModel>
}
