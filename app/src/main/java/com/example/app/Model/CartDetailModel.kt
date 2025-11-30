package com.example.app.Model

data class CartDetailModel(
    val id: Long,
    val productId: Long?,
    val cartId: Long?,
    val quantity: Int,
    val unitPrice: Double,
    val createAt: String?,
    val updateAt: String?
)
