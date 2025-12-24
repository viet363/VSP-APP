package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Model.OrderItemModel
import com.example.app.R
import java.text.NumberFormat
import java.util.Locale

class OrderItemAdapter(private val items: List<OrderItemModel>) :
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