package com.example.app.Model

data class CartResponse(
    val success: Boolean,
    val items: List<CartServerItem>
)

data class CartServerItem(
    val id: Long,
    val productId: Long,
    val productName: String,
    val price: Double,
    val image: String?,
    val quantity: Int
)
