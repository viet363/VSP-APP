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
    val Order_status: String?,
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

data class OrderRequest(
    val addressId: Int,
    val note: String? = null,
    val paymentMethod: String,
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
    val price: Double
)

data class OrderResponse(
    val success: Boolean,
    val orderId: Long,
    val message: String
)
data class OrdersResponse(
    val success: Boolean,
    val orders: List<OrderModel> = emptyList(),
    val message: String? = null
)

data class VNPayRequest(
    val orderId: Long
)

data class VNPayResponse(
    val success: Boolean,
    val paymentUrl: String,
    val orderId: Long,
    val amount: Double
)
data class MoMoRequest(
    val orderId: Long,
    val amount: Double,
    val orderInfo: String = "Thanh toán đơn hàng",
    val extraData: String = "",
    val requestType: String = "captureWallet"
)
data class MoMoResponse(
    val success: Boolean,
    val payUrl: String,
    val orderId: Long,
    val amount: Double,
    val message: String? = null
)
