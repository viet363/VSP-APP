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
    val picUrl: List<String>, // Đang là List<String>
    var rating: Double? = null,
    val score: Float? = 0f,
    var numberInCart: Int = 1,
    val isRecommended: Boolean = false
) : Serializable {

    @JvmOverloads
    constructor(
        id: Int,
        title: String?,
        description: String?,
        price: Double,
        picUrlString: String?,
        rating: Double? = null,
        score: Float? = 0f,
        numberInCart: Int = 1,
        isRecommended: Boolean = false
    ) : this(
        id = id,
        title = title,
        description = description,
        price = price,
        picUrl = if (picUrlString.isNullOrEmpty()) emptyList() else listOf(picUrlString),
        rating = rating,
        score = score,
        numberInCart = numberInCart,
        isRecommended = isRecommended
    )
}

fun ItemsModel.getFormattedRating(): String {
    return if ((this.rating ?: 0.0) > 0) {
        String.format("%.1f", this.rating)
    } else {
        "0.0"
    }
}