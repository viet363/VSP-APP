package com.example.app.Model

data class OrderModel(
    val timestamp: Long = 0,
    val items: List<ItemsModel> = emptyList(),
    val total: Double = 0.0,
    val status: String = ""
)