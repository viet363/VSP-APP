package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class ProductSpecificationModel(
    @SerializedName("id")
    val id: Long,

    @SerializedName("ProductId")
    val productId: Long,

    @SerializedName("spec_key")
    val specKey: String,

    @SerializedName("spec_value")
    val specValue: String
)

data class ProductSpecResponse(
    val success: Boolean,
    val count: Int,
    val data: List<ProductSpecificationModel>
)