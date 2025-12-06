package com.example.app.Helper

import android.content.Context
import android.util.Log
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

    companion object {
        private const val TAG = "ManagmentCart"
    }

    // Lấy danh sách CartItemModel từ TinyDB
    fun getLocalCart(): ArrayList<CartItemModel> {
        val json = tinyDB.getString(cartKey)
        return if (json.isNotEmpty()) {
            try {
                val type = object : TypeToken<ArrayList<CartItemModel>>() {}.type
                gson.fromJson(json, type) ?: arrayListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing cart from TinyDB", e)
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
                price = s.Price,
                picUrl = if (s.productImage != null) listOf(s.productImage) else emptyList(),
                rating = 0.0,
                numberInCart = s.Quantity
            )

            list.add(
                CartItemModel(
                    cartDetailId = s.Id,
                    item = item,
                    quantity = s.Quantity
                )
            )
        }

        saveLocalCart(list)
        Log.d(TAG, "Synced ${serverItems.size} items from server to local")
    }

    fun getTotalFee(): Double {
        val list = getLocalCart()
        return list.sumOf { it.item.price * it.quantity }
    }

    fun getItemCount(): Int {
        return getLocalCart().sumOf { it.quantity }
    }

    // Thêm item vào cart local
    fun addToLocal(item: ItemsModel, quantity: Int = 1) {
        val list = getLocalCart()
        val existingIndex = list.indexOfFirst { it.item.id == item.id }

        if (existingIndex >= 0) {
            list[existingIndex].quantity += quantity
        } else {
            list.add(CartItemModel(
                cartDetailId = 0L,
                item = item,
                quantity = quantity
            ))
        }

        saveLocalCart(list)
        Log.d(TAG, "Added item ${item.id} to local cart, quantity: $quantity")
    }

    fun updateLocalQuantity(itemId: Int, quantity: Int) {
        val list = getLocalCart()
        val index = list.indexOfFirst { it.item.id == itemId }

        if (index >= 0) {
            list[index].quantity = quantity
            saveLocalCart(list)
            Log.d(TAG, "Updated quantity for item $itemId to $quantity")
        } else {
            Log.w(TAG, "Item $itemId not found in local cart")
        }
    }

    fun removeFromLocal(itemId: Int) {
        val list = getLocalCart()
        val index = list.indexOfFirst { it.item.id == itemId }

        if (index >= 0) {
            list.removeAt(index)
            saveLocalCart(list)
            Log.d(TAG, "Removed item $itemId from local cart")
        } else {
            Log.w(TAG, "Item $itemId not found in local cart")
        }
    }

    // ===== THÊM METHOD clearCart() Ở ĐÂY =====
    fun clearCart() {
        // Xóa giỏ hàng local
        tinyDB.remove(cartKey)
        Log.d(TAG, "Cleared local cart (using clearCart method)")
    }

    fun clearLocalCart() {
        // Phương thức này vẫn giữ để tương thích
        tinyDB.remove(cartKey)
        Log.d(TAG, "Cleared local cart")
    }
    // ========================================

    fun getCartCount(): Int {
        return getLocalCart().sumOf { it.quantity }
    }

    // Sửa theo BE: Không cần userId trong params
    fun fetchFromServer(callback: (success: Boolean, items: List<CartServerItem>?, message: String?) -> Unit) {
        RetrofitClient.cartApi().getCart()
            .enqueue(object : Callback<CartResponse> {
                override fun onResponse(
                    call: Call<CartResponse>,
                    response: Response<CartResponse>
                ) {
                    if (response.isSuccessful) {
                        val cartResponse = response.body()
                        if (cartResponse?.success == true) {
                            val items = cartResponse.items
                            syncFromServer(items)
                            Log.d(TAG, "Fetched ${items.size} items from server")
                            callback(true, items, null)
                        } else {
                            Log.w(TAG, "Server returned unsuccessful response")
                            callback(false, null, "Không thể lấy giỏ hàng")
                        }
                    } else {
                        Log.e(TAG, "Server error: ${response.code()}")
                        callback(false, null, "Server error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                    Log.e(TAG, "Network error fetching cart", t)
                    callback(false, null, "Network error: ${t.message}")
                }
            })
    }

    fun addToCartServer(productId: Long, quantity: Int = 1, callback: (success: Boolean, message: String?) -> Unit) {
        val body = hashMapOf<String, Any>(
            "productId" to productId,
            "quantity" to quantity
        )

        RetrofitClient.cartApi().addToCart(body)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(
                    call: Call<CommonResponse>,
                    response: Response<CommonResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val success = result?.success ?: false
                        val message = result?.message

                        if (success) {
                            Log.d(TAG, "Added item $productId to server cart")
                            callback(true, message)
                        } else {
                            Log.w(TAG, "Failed to add item to server cart: $message")
                            callback(false, message ?: "Thêm vào giỏ hàng thất bại")
                        }
                    } else {
                        Log.e(TAG, "Server error: ${response.code()}")
                        callback(false, "Server error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    Log.e(TAG, "Network error", t)
                    callback(false, "Network error: ${t.message}")
                }
            })
    }

    fun updateCartServer(cartDetailId: Long, quantity: Int, callback: (success: Boolean, message: String?) -> Unit) {
        val body = hashMapOf<String, Any>("quantity" to quantity)

        RetrofitClient.cartApi().updateCart(cartDetailId, body)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(
                    call: Call<CommonResponse>,
                    response: Response<CommonResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val success = result?.success ?: false
                        val message = result?.message

                        if (success) {
                            Log.d(TAG, "Updated cart item $cartDetailId quantity to $quantity")
                            callback(true, message)
                        } else {
                            Log.w(TAG, "Failed to update cart item: $message")
                            callback(false, message ?: "Cập nhật số lượng thất bại")
                        }
                    } else {
                        Log.e(TAG, "Server error: ${response.code()}")
                        callback(false, "Server error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    Log.e(TAG, "Network error", t)
                    callback(false, "Network error: ${t.message}")
                }
            })
    }

    fun deleteFromServer(cartDetailId: Long, callback: (success: Boolean, message: String?) -> Unit) {
        RetrofitClient.cartApi().deleteCartItem(cartDetailId)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(
                    call: Call<CommonResponse>,
                    response: Response<CommonResponse>
                ) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val success = result?.success ?: false
                        val message = result?.message

                        if (success) {
                            Log.d(TAG, "Deleted cart item $cartDetailId from server")
                            callback(true, message)
                        } else {
                            Log.w(TAG, "Failed to delete cart item: $message")
                            callback(false, message ?: "Xóa sản phẩm thất bại")
                        }
                    } else {
                        Log.e(TAG, "Server error: ${response.code()}")
                        callback(false, "Server error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    Log.e(TAG, "Network error", t)
                    callback(false, "Network error: ${t.message}")
                }
            })
    }


    fun getCartSummary(): CartSummary {
        val list = getLocalCart()
        val totalItems = list.sumOf { it.quantity }
        val totalPrice = list.sumOf { it.item.price * it.quantity }

        return CartSummary(
            totalItems = totalItems,
            totalPrice = totalPrice,
            items = list.map { it.item.id }
        )
    }

    data class CartSummary(
        val totalItems: Int,
        val totalPrice: Double,
        val items: List<Int>
    )
}