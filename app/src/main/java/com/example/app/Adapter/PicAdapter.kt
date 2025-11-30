package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.ViewholderPicBinding

class PicAdapter(
    val items: MutableList<String>,
    private val onImageSelected: (String) -> Unit
) : RecyclerView.Adapter<PicAdapter.ViewHolder>() {

    private var selectedPosition = -1
    private var lastSelectedPosition = -1

    inner class ViewHolder(val binding: ViewholderPicBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderPicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.pic.loadImage(item)

        updateItemAppearance(holder, position)

        holder.binding.root.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                lastSelectedPosition = selectedPosition
                selectedPosition = adapterPosition

                if (lastSelectedPosition != -1) {
                    notifyItemChanged(lastSelectedPosition)
                }
                notifyItemChanged(selectedPosition)

                onImageSelected(items[adapterPosition])
            }
        }
    }

    private fun updateItemAppearance(holder: ViewHolder, position: Int) {
        if (selectedPosition == position) {
            holder.binding.picLayout.setBackgroundResource(R.drawable.green_bg_selected)
        } else {
            holder.binding.picLayout.setBackgroundResource(R.drawable.grey_bg)
        }
    }

    fun ImageView.loadImage(url: String) {
        Glide.with(this.context).load(url).into(this)
    }

    fun getSelectedImage(): String? {
        return if (selectedPosition != -1 && selectedPosition < items.size) {
            items[selectedPosition]
        } else {
            null
        }
    }

    fun getSelectedPosition(): Int {
        return selectedPosition
    }

    fun setSelectedPosition(position: Int) {
        if (position in 0 until items.size) {
            lastSelectedPosition = selectedPosition
            selectedPosition = position

            if (lastSelectedPosition != -1) {
                notifyItemChanged(lastSelectedPosition)
            }
            notifyItemChanged(selectedPosition)

            onImageSelected(items[position])
        }
    }

    fun setSelectedImage(imageUrl: String) {
        val position = items.indexOf(imageUrl)
        if (position != -1) {
            setSelectedPosition(position)
        }
    }
}