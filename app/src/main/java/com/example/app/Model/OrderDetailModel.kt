package com.example.app.Model

data class OrderDetailModel(
    val id: Long,
    val orderId: Long?,
    val productId: Long?,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercentage: Double?,
    val discountAmount: Double?
)
