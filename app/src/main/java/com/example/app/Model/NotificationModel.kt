package com.example.app.Model

data class NotificationModel(
    val id: Long,
    val userId: Long,
    val title: String?,
    val message: String?,
    val isRead: Boolean,
    val createdAt: String?
)