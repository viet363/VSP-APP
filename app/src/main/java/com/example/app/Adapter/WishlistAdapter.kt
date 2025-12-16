package com.example.app.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Model.WishlistModel
import com.example.app.R
import org.json.JSONArray

class WishlistAdapter(
    private var items: MutableList<WishlistModel>,
    private val onItemClick: (WishlistModel) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.ViewHolder>() {

    var onRemoveClickListener: ((Long, Int) -> Unit)? = null
    private val TAG = "WishlistAdapter"

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtPrice: TextView = itemView.findViewById(R.id.txtPrice)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_wishlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        Log.d(TAG, "Binding item at position $position: ${item.title}")

        // Xử lý hình ảnh theo cách mới
        getFirstImageUrl(item.picUrl)?.let { imageUrl ->
            if (imageUrl.isNotBlank()) {
                Log.d(TAG, "Loading image with Glide: $imageUrl")
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.grey_bg)
                    .error(R.drawable.grey_bg)
                    .into(holder.imgProduct)
            } else {
                Log.w(TAG, "Image URL is blank, using placeholder")
                holder.imgProduct.setImageResource(R.drawable.grey_bg)
            }
        } ?: run {
            Log.w(TAG, "No image URL available, using placeholder")
            holder.imgProduct.setImageResource(R.drawable.grey_bg)
        }

        // Hiển thị tên sản phẩm
        holder.txtName.text = item.title ?: "Không có tên"

        // Hiển thị giá
        val price = item.price ?: 0
        holder.txtPrice.text = "${price}₫"

        // Xử lý click vào sản phẩm
        holder.itemView.setOnClickListener {
            Log.d(TAG, "Item clicked: ${item.title}")
            onItemClick(item)
        }

        // Xử lý click xóa
        holder.btnRemove.setOnClickListener {
            Log.d(TAG, "Remove button clicked for product: ${item.productId}")
            onRemoveClickListener?.invoke(item.productId ?: 0L, position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<WishlistModel>) {
        Log.d(TAG, "Updating adapter data with ${newItems.size} items")
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
            Log.d(TAG, "Item removed at position $position")
        }
    }

    private fun getFirstImageUrl(picUrl: Any?): String? {
        Log.d(TAG, "getFirstImageUrl called with: $picUrl")
        Log.d(TAG, "Type of picUrl: ${picUrl?.javaClass?.simpleName}")

        return when (picUrl) {
            null -> {
                Log.d(TAG, "picUrl is null")
                null
            }
            is List<*> -> {
                Log.d(TAG, "picUrl is List, size: ${(picUrl as List<*>).size}")
                val firstItem = (picUrl as List<*>).firstOrNull()
                Log.d(TAG, "First item in list: $firstItem")
                firstItem?.toString()
            }
            is String -> {
                Log.d(TAG, "picUrl is String, length: ${picUrl.length}")
                Log.d(TAG, "String content (first 200 chars): ${picUrl.take(200)}")

                try {
                    if (picUrl.startsWith("[")) {
                        Log.d(TAG, "String starts with [, parsing as JSON array")
                        val jsonArray = JSONArray(picUrl)
                        Log.d(TAG, "JSON array length: ${jsonArray.length()}")
                        if (jsonArray.length() > 0) {
                            val result = jsonArray.getString(0)
                            Log.d(TAG, "Extracted from JSON array: $result")

                            // Kiểm tra nếu URL là relative path
                            return if (!result.startsWith("http")) {
                                val baseUrl = "http://192.168.1.100:4000"  // Thay bằng base URL của bạn
                                val fullUrl = if (result.startsWith("/")) {
                                    "$baseUrl$result"
                                } else {
                                    "$baseUrl/$result"
                                }
                                Log.d(TAG, "Converted to full URL: $fullUrl")
                                fullUrl
                            } else {
                                result
                            }
                        } else {
                            Log.d(TAG, "JSON array is empty")
                            null
                        }
                    } else {
                        Log.d(TAG, "String does not start with [, using as direct URL")

                        // Kiểm tra nếu URL là relative path
                        return if (!picUrl.startsWith("http")) {
                            val baseUrl = "http://192.168.1.100:4000"  // Thay bằng base URL của bạn
                            val fullUrl = if (picUrl.startsWith("/")) {
                                "$baseUrl$picUrl"
                            } else {
                                "$baseUrl/$picUrl"
                            }
                            Log.d(TAG, "Converted to full URL: $fullUrl")
                            fullUrl
                        } else {
                            picUrl
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON array: ${e.message}")

                    // Nếu parse lỗi, kiểm tra nếu URL là relative path
                    return if (!picUrl.startsWith("http")) {
                        val baseUrl = "http://192.168.1.100:4000"  // Thay bằng base URL của bạn
                        val fullUrl = if (picUrl.startsWith("/")) {
                            "$baseUrl$picUrl"
                        } else {
                            "$baseUrl/$picUrl"
                        }
                        Log.d(TAG, "Converted to full URL after error: $fullUrl")
                        fullUrl
                    } else {
                        picUrl
                    }
                }
            }
            else -> {
                Log.w(TAG, "Unknown picUrl type: ${picUrl.javaClass.simpleName}")
                null
            }
        }
    }
}