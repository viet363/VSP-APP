package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String?,
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

data class RegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("id") val id: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("fullname") val fullname: String?,
    @SerializedName("token") val token: String?
)

data class DirectLoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("id") val id: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("fullname") val fullname: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("birthday") val birthday: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("createAt") val createAt: String?,
    @SerializedName("updateAt") val updateAt: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("loginType") val loginType: String?
) {
    fun toUserData(): UserData = UserData(
        id = id ?: 0L,
        username = username ?: "",
        email = email,
        fullname = fullname,
        gender = gender,
        birthday = birthday,
        avatar = avatar,
        phone = phone,
        createAt = createAt,
        updateAt = updateAt,
        token = token,
        loginType = loginType
    )
}

data class ProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: UserData?,
    @SerializedName("message") val message: String? = null
)
data class NestedUserResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: UserData?
)