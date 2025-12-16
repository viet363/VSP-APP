package com.example.app.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Model.OrderItemModel
import com.example.app.Model.OrderModel
import com.example.app.R
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter(private var orders: List<OrderModel>) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val TAG = "OrderAdapter"

    fun updateData(newOrders: List<OrderModel>) {
        this.orders = newOrders
        notifyDataSetChanged()
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTxt: TextView = itemView.findViewById(R.id.dateTxt)
        val itemsRecyclerView: RecyclerView = itemView.findViewById(R.id.itemsRecyclerView)
        val totalTxt: TextView = itemView.findViewById(R.id.totalTxt)
        val statusTxt: TextView = itemView.findViewById(R.id.statusTxt)
        val addressTxt: TextView = itemView.findViewById(R.id.addressTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        Log.d(TAG, "Binding order at position $position, ID: ${order.Id}, Status: ${order.Order_status}")

        val orderDate = order.Order_date
        holder.dateTxt.text = "Ngày đặt: $orderDate"

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val total = when {
            order.total != null -> order.total!!
            order.Ship_fee.isNotBlank() -> order.Ship_fee.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        holder.totalTxt.text = "Tổng: ${formatter.format(total)}đ"

        holder.statusTxt.text = "Trạng thái: ${getStatusText(order.Order_status)}"

        val address = order.Ship_address ?: "Không có địa chỉ"
        holder.addressTxt.text = "Địa chỉ: $address"

        if (order.items.isNullOrEmpty()) {
            holder.itemsRecyclerView.visibility = View.GONE
        } else {
            holder.itemsRecyclerView.visibility = View.VISIBLE
            holder.itemsRecyclerView.layoutManager = LinearLayoutManager(
                holder.itemView.context,
                LinearLayoutManager.VERTICAL,
                false
            )
            holder.itemsRecyclerView.adapter = OrderItemAdapter(order.items)
        }
    }

    override fun getItemCount(): Int = orders.size

    private fun getStatusText(status: String?): String {
        return when (status?.trim()?.lowercase()) {
            "pending" -> "Chờ xác nhận"
            "processing" -> "Đang xử lý"
            "shipped" -> "Đang giao hàng"
            "delivered" -> "Đã giao"
            "cancelled" -> "Đã hủy"
            "returned" -> "Đã trả hàng"
            null -> "Không xác định"
            else -> status
        }
    }

    private class OrderItemAdapter(private val items: List<OrderItemModel>) :
        RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder>() {

        class OrderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleTxt: TextView = itemView.findViewById(R.id.titleTxt)
            val priceTxt: TextView = itemView.findViewById(R.id.priceTxt)
            val quantityTxt: TextView = itemView.findViewById(R.id.quantityTxt)
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_order_product, parent, false)
            return OrderItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
            val item = items[position]

            holder.titleTxt.text = item.Product_name ?: "Sản phẩm"

            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            formatter.minimumFractionDigits = 0
            formatter.maximumFractionDigits = 0
            val price = item.Unit_price ?: 0.0
            holder.priceTxt.text = "Giá: ${formatter.format(price)}đ"

            holder.quantityTxt.text = "Số lượng: ${item.Quantity}"

            item.picUrl?.let { imageUrl ->
                if (imageUrl.isNotBlank()) {
                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(holder.imageView)
                } else {
                    holder.imageView.setImageResource(R.drawable.placeholder)
                }
            } ?: run {
                holder.imageView.setImageResource(R.drawable.placeholder)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}