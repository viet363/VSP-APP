package com.example.app.Model

data class CommonResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null,
    val error: String? = null
)