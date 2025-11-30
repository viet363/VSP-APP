package com.example.app.Adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Activity.ListItemsActivity
import com.example.app.Model.CategoryModel
import com.example.app.R
import com.example.app.databinding.ViewhoderCategoryBinding

class CategoryAdapter(private val items: MutableList<CategoryModel>) :
    RecyclerView.Adapter<CategoryAdapter.Viewholder>() {

    private var selectedPosition = -1
    private var lastSelectedPosition = -1

    inner class Viewholder(val binding: ViewhoderCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val binding = ViewhoderCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]

        // Sửa tại đây
        holder.binding.titleTxt.text = item.categoryName

        Glide.with(holder.itemView.context)
            .load(item.picUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.binding.pic)

        if (selectedPosition == position) {
            holder.binding.pic.setBackgroundResource(0)
            holder.binding.mainLayout.setBackgroundResource(R.drawable.green_button_bg)

            ImageViewCompat.setImageTintList(
                holder.binding.pic,
                ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                )
            )

            holder.binding.titleTxt.visibility = View.VISIBLE
            holder.binding.titleTxt.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
        } else {
            holder.binding.pic.setBackgroundResource(R.drawable.grey_bg)
            holder.binding.mainLayout.setBackgroundResource(0)

            ImageViewCompat.setImageTintList(
                holder.binding.pic,
                ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.context, R.color.black)
                )
            )

            holder.binding.titleTxt.visibility = View.GONE
            holder.binding.titleTxt.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.black)
            )
        }

        holder.binding.root.setOnClickListener {
            // Sửa lỗi tại đây: thay adapterPosition bằng bindingAdapterPosition
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {

                lastSelectedPosition = selectedPosition
                selectedPosition = holder.bindingAdapterPosition

                notifyItemChanged(lastSelectedPosition)
                notifyItemChanged(selectedPosition)

                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(
                        holder.itemView.context,
                        ListItemsActivity::class.java
                    ).apply {
                        putExtra("id", item.id.toString())
                        putExtra("title", item.categoryName)   // sửa tại đây
                    }
                    ContextCompat.startActivity(
                        holder.itemView.context,
                        intent,
                        null
                    )
                }, 300)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}