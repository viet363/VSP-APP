package com.example.app.Model

data class ProductResponse(
    val Id: Int,
    val Product_name: String,
    val Description: String?,
    val Price: Double,
    val picUrl: String?,
    val CategoryId: Int?,
    val Category_name: String?
)
fun ProductResponse.toItemModel() = ItemsModel(
    id = Id,
    title = Product_name,
    description = Description,
    price = Price,
    picUrl = if (picUrl.isNullOrEmpty()) emptyList() else listOf(picUrl),
    rating = 0.0 // Nếu chưa có rating
)
