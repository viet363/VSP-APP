package com.example.app.Model

data class ProductRecommendationModel(
    val id: Long,
    val baseProductId: Long,
    val recommendedProductId: Long,
    val score: Double,
    val algorithm: String,
    val createdAt: String?,
    val updatedAt: String?
)
