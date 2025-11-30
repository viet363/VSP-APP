package com.example.app.Model

data class ProductReviewModel(
    val id: Long,
    val userId: Long?,
    val productId: Long?,
    val orderId: Long?,
    val rating: Int,
    val title: String?,
    val content: String?,
    val createAt: String?,
    val updateAt: String?
)
