package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class CategoryResponse(
    val success: Boolean,
    val data: List<CategoryModel>
)

data class CategoryModel(
    @SerializedName("Id")
    val id: Int,

    @SerializedName("Category_name")
    val name: String,

    @SerializedName("picUrl")
    val imageUrl: String?,

    @SerializedName("Create_at")
    val createdAt: String? = null,

    @SerializedName("Update_at")
    val updatedAt: String? = null
)