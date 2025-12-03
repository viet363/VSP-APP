package com.example.app.Model

data class UserModel(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val user: UserData? = null
)

data class UserData(
    val id: Long?,
    val username: String?,
    val email: String?,
    val fullname: String?,
    val gender: String?,
    val birthday: String?,
    val avatar: String?,
    val phone: String?,
    val createAt: String?,
    val updateAt: String?,
    val loginType: String?
)
