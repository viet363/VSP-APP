package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.app.Model.ProductSpecificationModel
import com.example.app.databinding.ItemSpecificationBinding

class SpecificationAdapter(
    private val items: List<ProductSpecificationModel>
) : RecyclerView.Adapter<SpecificationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSpecificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSpecificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Sửa tên field từ model mới
        holder.binding.keyTxt.text = item.specKey
        holder.binding.valueTxt.text = item.specValue
    }

    override fun getItemCount(): Int = items.size
}