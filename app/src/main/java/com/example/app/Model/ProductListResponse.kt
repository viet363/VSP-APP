package com.example.app.Model

data class ProductListResponse(
    val success: Boolean,
    val count: Int,
    val data: List<ProductResponse>?
)
