package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class ProductListResponse(
    val success: Boolean,

    @SerializedName("data")
    val data: List<ProductModel>? = null,  // ← API search/all trả về "data"

    @SerializedName("products")
    val products: List<ProductModel>? = null  // ← API category trả về "products"
) {
    fun getProductList(): List<ProductModel> {
        // Xử lý cả 2 trường hợp: "data" hoặc "products"
        return when {
            !data.isNullOrEmpty() -> data
            !products.isNullOrEmpty() -> products
            else -> emptyList()
        }
    }
}

data class ProductModel(
    @SerializedName("Id")
    val id: Int,

    @SerializedName("Product_name")
    val productName: String,

    @SerializedName("Description")
    val description: String?,

    @SerializedName("Price")
    val priceString: String,

    @SerializedName("picUrl")
    val picUrl: String?,

    @SerializedName("CategoryId")
    val categoryId: Int,

    @SerializedName("Category_name")
    val categoryName: String?
) {
    fun toItemModel(): ItemsModel {
        return ItemsModel(
            id = id,
            title = productName,
            description = description,
            price = priceString.toDoubleOrNull() ?: 0.0,
            picUrl = if (!picUrl.isNullOrEmpty()) listOf(picUrl) else emptyList()
        )
    }
}