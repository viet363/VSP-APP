package com.example.app.Model

data class UserInteractionModel(
    val id: Long,
    val userId: Long,
    val productId: Long,
    val interactionType: String?,
    val score: Int,
    val createdAt: String?
)
