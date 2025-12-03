package com.example.app.Model

import java.util.Date

data class OrderModel(
    val id: Long,
    val userId: Long,
    val orderDate: Date,
    val shippedDate: Date?,
    val note: String?,
    val shipAddress: String,
    val shipFee: Double,
    val paidDate: Date?,
    val orderStatus: String,
    val paymentType: String,
    val createAt: Date,
    val updateAt: Date,
    val addressId: Long?,
    val total: Double,
    val items: List<OrderItemModel> = emptyList()
)

data class OrderItemModel(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercentage: Double?,
    val discountAmount: Double?,
    val productName: String?,
    val productImage: String?
)

data class OrderDetailModel(
    val order: OrderModel,
    val items: List<OrderItemModel>
)
data class OrderResponse(
    val success: Boolean,
    val orders: List<OrderModel>,
    val message: String? = null
)
data class OrderDetailResponse(
    val success: Boolean,
    val order: OrderDetailModel,
    val message: String? = null
)