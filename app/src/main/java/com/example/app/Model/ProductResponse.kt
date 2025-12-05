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

    val rating: Double = 0.0
}

fun ProductResponse.toItemModel() = ItemsModel(
    id = id.toInt(),
    title = productName,
    description = description,
    price = price,
    picUrl = if (picUrl.isNullOrEmpty()) emptyList() else listOf(picUrl),
    rating = 4.9
)