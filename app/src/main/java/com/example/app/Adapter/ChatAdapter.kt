package com.example.app.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.Model.ChatMessage
import com.example.app.R  // ← THÊM DÒNG NÀY
import com.example.app.databinding.ItemChatLeftBinding
import com.example.app.databinding.ItemChatRightBinding

class ChatAdapter(private val chatList: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_RIGHT = 1
        private const val VIEW_TYPE_LEFT = 2
    }

    private var currentUserId: Long? = null

    fun setCurrentUserId(userId: Long) {
        currentUserId = userId
    }

    inner class ChatRightViewHolder(val binding: ItemChatRightBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatMessage) {
            binding.titleTxt.text = chat.message

            // Hiển thị thời gian
            val timeText = formatTime(chat.createdAt)
            if (binding.timeTxt != null && timeText.isNotEmpty()) {
                binding.timeTxt.text = timeText
                binding.timeTxt.visibility = android.view.View.VISIBLE
            }
        }
    }

    inner class ChatLeftViewHolder(val binding: ItemChatLeftBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatMessage) {
            binding.titleTxt.text = chat.message



            // Hiển thị thời gian
            val timeText = formatTime(chat.createdAt)
            if (binding.timeTxt != null && timeText.isNotEmpty()) {
                binding.timeTxt.text = timeText
                binding.timeTxt.visibility = android.view.View.VISIBLE
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chat = chatList[position]
        // Sử dụng senderType từ server hoặc so sánh senderId
        return if (chat.senderType == "user" || (currentUserId != null && chat.senderId == currentUserId)) {
            VIEW_TYPE_RIGHT
        } else {
            VIEW_TYPE_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_RIGHT) {
            val binding = ItemChatRightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ChatRightViewHolder(binding)
        } else {
            val binding = ItemChatLeftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ChatLeftViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chatList[position]

        when (holder) {
            is ChatRightViewHolder -> holder.bind(chat)
            is ChatLeftViewHolder -> holder.bind(chat)
        }
    }

    override fun getItemCount(): Int = chatList.size

    // Helper function để format thời gian
    private fun formatTime(dateString: String): String {
        return try {
            // Các format có thể có
            val formats = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.SSS"
            )

            var parsedDate: java.util.Date? = null
            for (format in formats) {
                try {
                    val inputFormat = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                    inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    parsedDate = inputFormat.parse(dateString)
                    if (parsedDate != null) break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }

            if (parsedDate != null) {
                val outputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                outputFormat.timeZone = java.util.TimeZone.getDefault()
                outputFormat.format(parsedDate)
            } else {
                // Fallback: extract time if format contains T
                if (dateString.contains("T")) {
                    val timePart = dateString.substring(dateString.indexOf("T") + 1)
                    timePart.substring(0, kotlin.math.min(5, timePart.length))
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }
}