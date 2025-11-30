package com.example.app.Model

data class WishlistModel(
    val id: Long,
    val userId: Long,
    val productId: Long,
    val createdAt: String?
)
