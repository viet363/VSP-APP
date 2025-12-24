package com.example.app.Model

data class NotificationModel(
    val id: Long,
    val userId: Long?,
    val title: String?,
    val message: String?,
    val type: String?,
    val referenceId: Long?,
    val isRead: Boolean,
    val createdAt: String?
)
data class NotificationResponse(
    val success: Boolean,
    val data: List<NotificationModel>
)