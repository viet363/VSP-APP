package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ChatData?,
    @SerializedName("message") val message: String?
)

data class ChatData(
    @SerializedName("admin") val admin: Admin?,
    @SerializedName("messages") val messages: List<ChatMessage>?
)

data class Admin(
    @SerializedName("Id") val id: Int,
    @SerializedName("Username") val username: String,
    @SerializedName("Fullname") val fullname: String,
    @SerializedName("Avatar") val avatar: String
)

data class ChatMessage(
    @SerializedName("Id") val id: Long,
    @SerializedName("UserId") val userId: Long,
    @SerializedName("AdminId") val adminId: Long,
    @SerializedName("SenderId") val senderId: Long,
    @SerializedName("Message") val message: String,
    @SerializedName("MessageType") val messageType: String,
    @SerializedName("IsRead") val isRead: Int,

    @SerializedName("Created_at") val createdAt: String,
    @SerializedName("ChatType") val chatType: String,

    @SerializedName("SenderType") val senderType: String?
)

// Response đơn giản cho sendMessage
data class BaseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: Any?
)