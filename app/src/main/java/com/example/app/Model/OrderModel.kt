package com.example.app.Model

import java.util.Date

data class OrderModel(
    val Id: Long,
    val UserId: Long,
    val Order_date: String,  // BE trả về Order_date
    val Shipped_date: String?,
    val Note: String?,
    val Ship_address: String,
    val Ship_fee: Double,
    val Paid_date: String?,
    val Order_status: String,
    val Payment_type: String,
    val Create_at: String,
    val Update_at: String,
    val AddressId: Long?,
    val total: Double,
    val items: List<OrderItemModel> = emptyList()
)

data class OrderItemModel(
    val Id: Long,
    val OrderId: Long,
    val ProductId: Long,
    val Quantity: Int,
    val Unit_price: Double,
    val Discount_percentage: Double?,
    val Discount_amount: Double?,
    val Product_name: String?,  // BE join từ bảng product
    val picUrl: String?  // BE join từ bảng product
)

data class OrderResponse(
    val success: Boolean,
    val orders: List<OrderModel>,
    val message: String? = null
)

data class OrderDetailResponse(
    val success: Boolean,
    val order: OrderModel,
    val message: String? = null
)