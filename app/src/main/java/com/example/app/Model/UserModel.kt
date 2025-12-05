package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullname") val fullname: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("birthday") val birthday: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("createAt") val createAt: String?,
    @SerializedName("updateAt") val updateAt: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("loginType") val loginType: String?
)

data class UserResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: UserData
)

// Giữ lại UserModel cũ cho tương thích nếu cần
data class UserModel(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("user") val user: UserData? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("id") val id: Long? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("fullname") val fullname: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("birthday") val birthday: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("createAt") val createAt: String? = null,
    @SerializedName("updateAt") val updateAt: String? = null,
    @SerializedName("loginType") val loginType: String? = null
)