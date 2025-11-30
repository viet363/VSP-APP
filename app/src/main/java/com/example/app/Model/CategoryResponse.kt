package com.example.app.Model

data class CategoryResponse(
    val Id: Int,
    val Category_name: String,
    val Description: String?,
    val picUrl: String?
)
fun CategoryResponse.toCategoryModel() = CategoryModel(
    id = Id.toLong(),
    categoryName = Category_name,
    picUrl = picUrl,
    createAt = null,
    updateAt = null
)
