package com.example.app.Model

import java.io.Serializable

data class RecommendResponse(
    val success: Boolean,
    val count: Int,
    val data: List<RecommendedProduct>?
)

data class RecommendedProduct(
    val Id: Int,
    val Product_name: String,
    val Description: String?,
    val Price: Double,
    val picUrl: String?,
    val Score: Float?
)

data class ItemsModel(
    val id: Int,
    val title: String?,
    val description: String?,
    val price: Double,
    val picUrl: List<String>,
    var rating: Double? = 0.0,
    val score: Float? = 0f,
    var numberInCart: Int = 1,
    val isRecommended: Boolean = false
) : Serializable {

    constructor(r: RecommendedProduct) : this(
        id = r.Id,
        title = r.Product_name,
        description = r.Description,
        price = r.Price,
        picUrl = if (!r.picUrl.isNullOrEmpty()) listOf(r.picUrl) else emptyList(),
        score = r.Score ?: 0f,
        numberInCart = 1,
        isRecommended = true
    )
}

