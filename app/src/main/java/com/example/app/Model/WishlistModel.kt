package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class WishlistResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<WishlistModel>? = null,

    @SerializedName("error")
    val error: String? = null
)

data class WishlistModel(
    @SerializedName("Id")
    val id: Long? = null,

    @SerializedName("ProductId")
    val productId: Long? = null,

    @SerializedName("Created_at")
    val createdAt: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("Description")
    val description: String? = null,

    @SerializedName("Price")
    val price: Int? = null,

    @SerializedName("picUrl")
    val picUrl: Any? = null,  // Có thể là String hoặc List<String>

    @SerializedName("Category_name")
    val categoryName: String? = null,

    @SerializedName("rating")
    val rating: Double? = null
) {
    // Helper function để lấy danh sách ảnh
    fun getImageUrls(): List<String> {
        return when (picUrl) {
            is String -> {
                try {
                    // Try to parse JSON string
                    if ((picUrl as String).startsWith("[")) {
                        // Parse JSON array
                        val jsonArray = org.json.JSONArray(picUrl)
                        (0 until jsonArray.length()).map { jsonArray.getString(it) }
                    } else {
                        listOf(picUrl as String)
                    }
                } catch (e: Exception) {
                    listOf(picUrl as String)
                }
            }
            is List<*> -> (picUrl as List<*>).mapNotNull { it?.toString() }
            else -> emptyList()
        }
    }

    override fun toString(): String {
        return "WishlistModel(id=$id, productId=$productId, title=$title, price=$price, picUrlType=${picUrl?.javaClass?.simpleName})"
    }
}