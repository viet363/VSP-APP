package com.example.app.Model

import java.io.Serializable

data class CartItemModel(
    val cartDetailId: Long? = null,
    val item: ItemsModel,
    var quantity: Int = 1
) : Serializable

data class CartResponse(
    val success: Boolean,
    val cartId: Long?,
    val items: List<CartServerItem>
)

data class CartServerItem(
    val Id: Long,
    val productId: Long,
    val productName: String,
    val Price: Double,  // Chú ý: BE trả về Price
    val productImage: String?,  // BE trả về productImage
    val Quantity: Int  // Chú ý: BE trả về Quantity
)