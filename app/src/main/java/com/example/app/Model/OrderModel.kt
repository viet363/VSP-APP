package com.example.app.Model

data class OrderModel(
    val Id: Int,
    val UserId: Int,
    val Order_date: String,
    val Shipped_date: String?,
    val Note: String?,
    val Ship_address: String,
    val Ship_fee: String,
    val Paid_date: String?,
    val Order_status: String,
    val Payment_type: String,
    val Create_at: String,
    val Update_at: String,
    val AddressId: Int?,
    val customer_name: String?,
    val total: Double? = null,
    val items: List<OrderItemModel> = emptyList()
)

data class OrderItemModel(
    val id: Int = 0,
    val Product_name: String? = null,
    val Unit_price: Double = 0.0,
    val Quantity: Int = 0,
    val picUrl: String? = null
)