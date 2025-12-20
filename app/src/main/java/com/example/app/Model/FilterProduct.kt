package com.example.app.Model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FilterApiResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<FilterProduct>?,

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("totalPages")
    val totalPages: Int = 1,

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("message")
    val message: String? = null
) : Serializable

data class FilterProduct(
    @SerializedName("Id")
    val id: Int,

    @SerializedName("Product_name")
    val productName: String?,

    @SerializedName("Description")
    val description: String?,

    @SerializedName("Price")
    private val priceString: String,

    @SerializedName("picUrl")
    val picUrl: String?,

    @SerializedName("CategoryId")
    val categoryId: Long?,

    @SerializedName("Category_name")
    val categoryName: String?,

    @SerializedName("avg_rating")
    private val avgRatingString: String?,

    @SerializedName("total_stock")
    private val totalStockString: String?
) : Serializable {

    val price: Double
        get() = priceString.toDoubleOrNull() ?: 0.0

    val avgRating: Double?
        get() = avgRatingString?.toDoubleOrNull()

    val totalStock: Int?
        get() = totalStockString?.toIntOrNull()

    val displayImageUrl: String?
        get() {
            return when {
                picUrl.isNullOrEmpty() -> null
                picUrl.startsWith("data:image") -> {
                    picUrl
                }
                else -> picUrl
            }
        }

    val isBase64Image: Boolean
        get() = !picUrl.isNullOrEmpty() && picUrl.startsWith("data:image")

    val base64Data: String?
        get() {
            return if (isBase64Image) {
                // Tách phần base64 sau dấu phẩy
                val commaIndex = picUrl!!.indexOf(",")
                if (commaIndex != -1) {
                    picUrl.substring(commaIndex + 1)
                } else {
                    picUrl
                }
            } else {
                null
            }
        }

    val imageMimeType: String?
        get() {
            return if (isBase64Image) {
                val mimeEnd = picUrl!!.indexOf(";")
                if (mimeEnd != -1) {
                    picUrl.substring(5, mimeEnd)
                } else {
                    "image/jpeg"
                }
            } else {
                null
            }
        }

    fun toItemsModel(): ItemsModel {
        return ItemsModel(
            id = id,
            title = productName,
            description = description,
            price = price,
            picUrlString = displayImageUrl,
            rating = avgRating,
            score = avgRating?.toFloat(),
            numberInCart = 1,
            isRecommended = false
        )
    }
}