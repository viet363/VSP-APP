package com.example.app.Model

data class ProductReviewModel(
    val id: Long,
    val userId: Long?,
    val productId: Long?,
    val orderId: Long?,
    val rating: Int,
    val title: String?,
    val content: String?,
    val createAt: String?,
    val updateAt: String?
) {
    val userName: String? = null
    val imageUrl: String? = null
    val videoUrl: String? = null

    fun toCommentModel(): CommentModel {
        return CommentModel(
            userName = this.userName ?: "Người dùng ${this.userId}",
            commentText = this.content ?: this.title ?: "",
            imageUrl = this.imageUrl,
            videoUrl = this.videoUrl
        )
    }
}
