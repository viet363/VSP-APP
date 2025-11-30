package com.example.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.databinding.ViewholderModelBinding

class SelectedModelAdapter(val items: MutableList<String>) : RecyclerView.Adapter<SelectedModelAdapter.ViewHolder>() {
    private var selectedPosition = -1
    private var lastSelectedPosition = -1
    private lateinit var context: Context

    inner class ViewHolder(val binding: ViewholderModelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding = ViewholderModelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.modelTxt.text = items[position]

        // Cập nhật giao diện dựa trên vị trí hiện tại
        updateItemAppearance(holder, position)

        holder.binding.root.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                lastSelectedPosition = selectedPosition
                selectedPosition = adapterPosition

                // Chỉ cập nhật các item thay đổi
                if (lastSelectedPosition != -1) {
                    notifyItemChanged(lastSelectedPosition)
                }
                notifyItemChanged(selectedPosition)
            }
        }
    }

    private fun updateItemAppearance(holder: ViewHolder, position: Int) {
        if (selectedPosition == position) {
            holder.binding.modelLayout.setBackgroundResource(R.drawable.green_bg_selected)
            holder.binding.modelTxt.setTextColor(ContextCompat.getColor(context, R.color.green))
        } else {
            holder.binding.modelLayout.setBackgroundResource(R.drawable.grey_bg)
            holder.binding.modelTxt.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // Thêm method để lấy item được chọn
    fun getSelectedItem(): String? {
        return if (selectedPosition != -1 && selectedPosition < items.size) {
            items[selectedPosition]
        } else {
            null
        }
    }

    // Thêm method để lấy vị trí được chọn
    fun getSelectedPosition(): Int {
        return selectedPosition
    }

    // Thêm method để thiết lập item được chọn từ bên ngoài
    fun setSelectedPosition(position: Int) {
        if (position in 0 until items.size) {
            lastSelectedPosition = selectedPosition
            selectedPosition = position

            if (lastSelectedPosition != -1) {
                notifyItemChanged(lastSelectedPosition)
            }
            notifyItemChanged(selectedPosition)
        }
    }
}