package com.example.app.Helper

import android.content.Context
import com.example.app.Model.*
import com.example.app.Network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManagmentCart(private val context: Context) {

    private val tinyDB = TinyDB(context)
    private val cartKey = "CART_LIST"
    private val gson = Gson()

    // Lấy danh sách CartItemModel từ TinyDB
    fun getLocalCart(): ArrayList<CartItemModel> {
        val json = tinyDB.getString(cartKey)
        return if (json.isNotEmpty()) {
            try {
                val type = object : TypeToken<ArrayList<CartItemModel>>() {}.type
                gson.fromJson(json, type) ?: arrayListOf()
            } catch (e: Exception) {
                e.printStackTrace()
                arrayListOf()
            }
        } else {
            arrayListOf()
        }
    }

    // Lưu danh sách CartItemModel vào TinyDB
    fun saveLocalCart(list: ArrayList<CartItemModel>) {
        val json = gson.toJson(list)
        tinyDB.putString(cartKey, json)
    }

    // Lấy danh sách ItemsModel (nếu cần tương thích với code cũ)
    fun getLocalItems(): ArrayList<ItemsModel> {
        val cartItems = getLocalCart()
        return ArrayList(cartItems.map { it.item })
    }

    /**
     * ĐỒNG BỘ TỪ SERVER VỀ LOCAL (TinyDB)
     */
    fun syncFromServer(serverItems: List<CartServerItem>) {
        val list = ArrayList<CartItemModel>()

        serverItems.forEach { s ->
            val item = ItemsModel(
                id = s.productId.toInt(),
                title = s.productName,
                description = "",
                price = s.price,
                picUrl = if (s.image != null) listOf(s.image) else emptyList(),
                rating = 0.0
            )

            list.add(
                CartItemModel(
                    cartDetailId = s.id,
                    item = item,
                    quantity = s.quantity
                )
            )
        }

        saveLocalCart(list)
    }

    fun getTotalFee(): Double {
        val list = getLocalCart()
        return list.sumOf { it.item.price * it.quantity }
    }

    // Thêm item vào cart local
    fun addToLocal(item: ItemsModel, quantity: Int = 1) {
        val list = getLocalCart()
        val existingIndex = list.indexOfFirst { it.item.id == item.id }

        if (existingIndex >= 0) {
            list[existingIndex].quantity += quantity
        } else {
            list.add(CartItemModel(
                cartDetailId = 0L, // 0 cho local items
                item = item,
                quantity = quantity
            ))
        }

        saveLocalCart(list)
    }

    fun updateLocalQuantity(itemId: Int, quantity: Int) {
        val list = getLocalCart()
        val index = list.indexOfFirst { it.item.id == itemId }

        if (index >= 0) {
            list[index].quantity = quantity
            saveLocalCart(list)
        }
    }

    fun removeFromLocal(itemId: Int) {
        val list = getLocalCart()
        val index = list.indexOfFirst { it.item.id == itemId }

        if (index >= 0) {
            list.removeAt(index)
            saveLocalCart(list)
        }
    }

    fun clearLocalCart() {
        tinyDB.remove(cartKey)
    }

    fun getCartCount(): Int {
        return getLocalCart().sumOf { it.quantity }
    }

    // Các phương thức server giữ nguyên
    fun addToCartServer(productId: Long, quantity: Int, callback: () -> Unit) {
        val body = hashMapOf<String, Any>(
            "productId" to productId,
            "quantity" to quantity
        )

        RetrofitClient.cartApi.addToCart(body)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) { callback() }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
            })
    }

    fun updateQuantityServer(cartDetailId: Long, quantity: Int, callback: () -> Unit) {
        val body = hashMapOf<String, Any>("quantity" to quantity)

        RetrofitClient.cartApi.updateCart(cartDetailId, body)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) { callback() }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
            })
    }

    fun deleteFromServer(cartDetailId: Long, callback: () -> Unit) {
        RetrofitClient.cartApi.deleteCartItem(cartDetailId)
            .enqueue(object : Callback<Map<String, Boolean>> {
                override fun onResponse(
                    call: Call<Map<String, Boolean>>,
                    response: Response<Map<String, Boolean>>
                ) { callback() }

                override fun onFailure(call: Call<Map<String, Boolean>>, t: Throwable) {}
            })
    }
}