package com.example.app.Model

import java.io.Serializable

data class FilterRequest(
    val categoryIds: List<Long>? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minRating: Int? = null,
    val inStock: Boolean? = null,
    val sortBy: String? = null,
    val keyword: String? = null,
    val page: Int = 1,
    val limit: Int = 20
) : Serializable

data class FilterResponse(
    val success: Boolean,
    val data: List<ItemsModel>? = null,
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val message: String? = null
) : Serializable