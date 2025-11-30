package com.example.app.Model

data class UserModel(
    val id: Long,
    val username: String,
    val password: String? = null,
    val email: String?,
    val fullname: String?,
    val gender: String?,
    val birthday: String?,
    val avatar: String?,
    val phone: String?,
    val createAt: String?,
    val updateAt: String?,
    val token: String? = null,
    val loginType: String? = "email",
    val googleId: String? = null
)