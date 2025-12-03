package com.example.app.Model

import java.io.Serializable

data class CartItemModel(
    val cartDetailId: Long? = null,
    val item: ItemsModel,
    var quantity: Int = 1
) : Serializable
