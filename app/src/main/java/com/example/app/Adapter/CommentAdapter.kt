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
        RecyclerView.ViewHolder(binding.root) {

        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarReview)

        fun bind(review: ProductReviewModel) {
            binding.txtNameComment.text = review.userName ?: "Người dùng ${review.userId ?: "Ẩn danh"}"
            binding.txtComment.text = review.content ?: review.title ?: ""

            ratingBar.rating = review.rating.toFloat()

            review.createAt?.let { dateStr ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date = inputFormat.parse(dateStr)
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    binding.txtDate.text = outputFormat.format(date)
                } catch (e: Exception) {
                    binding.txtDate.text = dateStr
                }
            } ?: run {
                binding.txtDate.text = ""
            }

            if (!review.userAvatar.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(review.userAvatar)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(binding.imgAvatar)
            } else {
                binding.imgAvatar.setImageResource(R.drawable.placeholder)
            }

            binding.commentImage.visibility = View.GONE
            binding.commentVideo.visibility = View.GONE
        }
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
        holder.bind(listComment[position])
    }

    fun updateData(newList: List<ProductReviewModel>) {
        listComment = newList
        notifyDataSetChanged()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.commentVideo.stopPlayback()
    }
}