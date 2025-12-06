package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Model.ProductReviewModel
import com.example.app.R
import com.example.app.databinding.ViewholderListCommentBinding
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private var listComment: List<ProductReviewModel> = emptyList()
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ViewholderListCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun updateData(newList: List<ProductReviewModel>) {
        listComment = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderListCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = listComment.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = listComment[position]
        val context = holder.itemView.context

        holder.binding.txtNameComment.text = review.userName ?: "Người dùng ${review.userId ?: "Ẩn danh"}"

        holder.binding.txtComment.text = review.content ?: review.title ?: ""

        if (review.rating > 0) {
            holder.binding.txtComment.append("\n\n⭐ ${review.rating}/5")
        }

        if (!review.userAvatar.isNullOrEmpty()) {
            Glide.with(context)
                .load(review.userAvatar)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.binding.imgAvatar)
        } else {
            holder.binding.imgAvatar.setImageResource(R.drawable.placeholder)
        }

        review.createAt?.let { dateStr ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            } catch (e: Exception) {
            }
        }

        holder.binding.commentImage.visibility = View.GONE
        holder.binding.commentVideo.visibility = View.GONE
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.commentVideo.stopPlayback()
    }
}