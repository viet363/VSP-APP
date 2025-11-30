package com.example.app.Model

data class ProductDiscountModel(
    val id: Long,
    val productId: Long?,
    val discountAmount: Double,
    val startDate: String?,
    val endDate: String?,
    val isActive: Boolean
)
