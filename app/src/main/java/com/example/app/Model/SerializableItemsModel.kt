package com.example.app.Model

import java.io.Serializable

data class SerializableItemsModel(
    val id: Long = 0L,
    val name: String = "",
    val price: Double = 0.0,
    val picUrl: List<String> = emptyList(),
    val rating: Float = 0f,
    val isRecommended: Boolean = false,
    val isBestseller: Boolean = false,
    val discount: Int = 0,
    val discountPrice: Double = 0.0
) : Serializable