package com.example.app.Model

import java.io.Serializable

data class RecommendResponse(
    val success: Boolean,
    val count: Int,
    val data: List<RecommendedProduct>
)

data class RecommendedProduct(
    val Id: Int,
    val Title: String,
    val Description: String?,
    val Price: Double,
    val ImageUrl: String?,
    val Rating: Double?
)

data class ItemsModel(
    val id: Int,
    val title: String,
    val description: String?,
    val price: Double,
    val picUrl: List<String>,
    val rating: Double? = 0.0
) : Serializable {

    constructor(r: RecommendedProduct) : this(
        id = r.Id,
        title = r.Title,
        description = r.Description,
        price = r.Price,
        picUrl = if (!r.ImageUrl.isNullOrEmpty()) listOf(r.ImageUrl) else emptyList(),
        rating = r.Rating ?: 0.0
    )
}
