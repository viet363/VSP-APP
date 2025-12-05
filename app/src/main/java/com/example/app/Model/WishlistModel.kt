package com.example.app.Model

data class WishlistModel(
    val productId: Long,
    val productName: String,
    val price: Double,
    val picUrl: String?,
    val createdAt: String?
)

data class WishlistResponse(
    val success: Boolean,
    val count: Int,
    val data: List<WishlistModel>
)