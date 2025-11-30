package com.example.app.Model

data class InventoryModel(
    val id: Long,
    val productId: Long,
    val warehouseId: Long,
    val stock: Int,
    val minStock: Int,
    val updatedAt: String?
)
