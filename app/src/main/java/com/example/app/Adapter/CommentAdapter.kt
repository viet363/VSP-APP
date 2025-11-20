package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Model.CommentModel
import com.example.app.databinding.ViewholderListCommentBinding

class CommentAdapter(
    private val listComment: ArrayList<CommentModel>,

) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    // ViewHolder chứa binding cho từng item
    inner class ViewHolder(val binding: ViewholderListCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Tạo ViewHolder từ layout XML
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderListCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    // Trả về số lượng bình luận
    override fun getItemCount(): Int = listComment.size

    // Gán dữ liệu cho từng item trong RecyclerView
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = listComment[position]
        val context = holder.itemView.context

        // Hiển thị tên người dùng và nội dung bình luận
        holder.binding.txtNameComment.text = comment.userName
        holder.binding.txtComment.text = comment.commentText

        // Hiển thị ảnh nếu có
        if (!comment.imageUrl.isNullOrEmpty()) {
            holder.binding.commentImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(comment.imageUrl)
                .into(holder.binding.commentImage)
        } else {
            holder.binding.commentImage.visibility = View.GONE
        }

        // Hiển thị video nếu có
        if (!comment.videoUrl.isNullOrEmpty()) {
            holder.binding.commentVideo.visibility = View.VISIBLE
            holder.binding.commentVideo.setVideoPath(comment.videoUrl)

            // Chỉ tạo MediaController một lần để tối ưu hiệu suất
            val mediaController = MediaController(context)
            mediaController.setAnchorView(holder.binding.commentVideo)
            holder.binding.commentVideo.setMediaController(mediaController)

            holder.binding.commentVideo.setOnPreparedListener { mp ->
                mp.isLooping = true
                holder.binding.commentVideo.start()
            }
        } else {
            holder.binding.commentVideo.visibility = View.GONE
        }


    }

    // Dừng video khi mục đã được tái sử dụng
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.commentVideo.stopPlayback() // Dừng video khi RecyclerView cuộn tới item khác
    }
}
