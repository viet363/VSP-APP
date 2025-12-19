package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    @SerializedName("Id")
    val id: Long,

    @SerializedName("Product_name")
    val productName: String,

    @SerializedName("model")
    val model: String?,

    @SerializedName("Description")
    val description: String?,

    @SerializedName("Price")
    val priceString: String,

    @SerializedName("picUrl")
    val picUrl: String?,

    @SerializedName("CategoryId")
    val categoryId: Long?,

    @SerializedName("Category_name")
    val categoryName: String?
) {
    val price: Double
        get() = priceString.toDoubleOrNull() ?: 0.0

    var rating: Double = 0.0

    constructor(
        id: Long,
        productName: String,
        model: String?,
        description: String?,
        priceString: String,
        picUrl: String?,
        categoryId: Long?,
        categoryName: String?,
        rating: Double
    ) : this(id, productName, model, description, priceString, picUrl, categoryId, categoryName) {
        this.rating = rating
    }

    fun copyWithRating(newRating: Double): ProductResponse {
        return ProductResponse(
            id = this.id,
            productName = this.productName,
            model = this.model,
            description = this.description,
            priceString = this.priceString,
            picUrl = this.picUrl,
            categoryId = this.categoryId,
            categoryName = this.categoryName,
            rating = newRating
        )
    }
}

fun ProductResponse.toItemModel() = ItemsModel(
    id = id.toInt(),
    title = productName,
    description = description,
    price = price,
    picUrl = if (picUrl.isNullOrEmpty()) emptyList() else listOf(picUrl),
    rating = rating
)