package com.example.app.Model

data class CouponModel(
    val id: Long,
    val code: String,
    val discountType: String,
    val discountValue: Double,
    val startDate: String,
    val endDate: String,
    val usageLimit: Int,
    val usedCount: Int,
    val isActive: Boolean
)
