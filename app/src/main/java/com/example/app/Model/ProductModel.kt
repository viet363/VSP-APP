package com.example.app.Model

data class ProductModel(
    val id: Long,
    val categoryId: Long?,
    val productName: String,
    val model: String?,
    val description: String?,
    val standardCost: Double?,
    val price: Double,
    val productStatus: String,
    val picUrl: String?,
    val createAt: String?,
    val updateAt: String?
)
