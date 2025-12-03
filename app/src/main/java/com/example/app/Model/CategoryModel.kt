package com.example.app.Model

data class CategoryModel(
    val id: Long,
    val name: String,                 
    val image_url: String?,
    val created_at: String? = null,
    val updated_at: String? = null
)