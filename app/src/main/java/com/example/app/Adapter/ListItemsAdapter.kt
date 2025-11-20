package com.example.app.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Activity.DetailActivity
import com.example.app.Model.ItemsModel
import com.example.app.databinding.ViewholderListItemBinding

class ListItemsAdapter (val items: MutableList<ItemsModel>) :
    RecyclerView.Adapter<ListItemsAdapter.Viewholder>() {

    class Viewholder(val binding: ViewholderListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int): Viewholder {
        val binding =
            ViewholderListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]

        with(holder.binding) {
            titleTxt.text = item.title
            priceTxt.text = "$${item.price}"
            ratingTxt.text = item.rating.toString()

            // Kiểm tra nếu picUrl không rỗng để tránh lỗi
            if (item.picUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(item.picUrl[0]) // Lấy hình đầu tiên trong danh sách
                    .into(pic)
            }

            // Xử lý sự kiện click vào item
            root.setOnClickListener {
                val intent = Intent(holder.itemView.context, DetailActivity::class.java).apply {
                    putExtra("object", item)
                    putExtra("itemKey", item.id)
                }
                ContextCompat.startActivity(holder.itemView.context, intent , null)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
