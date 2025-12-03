package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.app.Model.ProductReviewModel
import com.example.app.databinding.ViewholderListCommentBinding

class CommentAdapter(
    private val listComment: List<ProductReviewModel>
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ViewholderListCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

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

        holder.binding.txtNameComment.text = "Người dùng ${review.userId ?: "Ẩn danh"}"
        holder.binding.txtComment.text = review.content ?: review.title ?: ""

        if (review.rating > 0) {
            holder.binding.txtComment.append("\n\nĐánh giá: ${review.rating}/5")
        }


        holder.binding.commentImage.visibility = View.GONE

        holder.binding.commentVideo.visibility = View.GONE

        if (!review.createAt.isNullOrEmpty()) {
            holder.binding.txtComment.append("\n\nNgày: ${review.createAt}")
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.commentVideo.stopPlayback()
    }
}