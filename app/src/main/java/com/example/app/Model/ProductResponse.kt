package com.example.app.Model

data class ProductResponse(
    val id: Int,
    val name: String,
    val description: String? =null ,
    val price: Double,
    val image_url: String?,
    val category_id: Int?,
    val category_name: String?,
    val rating: Double?
)
fun ProductResponse.toItemModel() = ItemsModel(
    id = id,
    title = name,
    description = description,
    price = price,
    picUrl = if (image_url.isNullOrEmpty()) emptyList() else listOf(image_url),
    rating = rating ?: 0.0
)
