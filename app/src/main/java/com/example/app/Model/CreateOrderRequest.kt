package com.example.app.Model

import java.math.BigDecimal

data class CreateOrderRequest(
    val addressId: Long,
    val paymentMethod: String,
    val note: String? = null,
    val shipFee: BigDecimal
)

data class CreateOrderResponse(
    val success: Boolean,
    val message: String? = null,
    val orderId: Long? = null,
    val totalAmount: Double? = null
)

data class OrderListResponse(
    val success: Boolean,
    val orders: List<OrderModel> = emptyList()
)
data class OrderDetailResponse(
    val success: Boolean,
    val order: OrderModel? = null
)