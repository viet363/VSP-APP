package com.example.app.Model

data class PaymentModel(
    val id: Long,
    val orderId: Long?,
    val paymentMethod: String?,
    val amount: Double,
    val paymentStatus: String,
    val transactionCode: String?,
    val paidAt: String?
)
