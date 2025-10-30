package com.example.coded.data

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val listingId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val isRead: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val createdAt: Timestamp = Timestamp.now(),
    val chatId: String = ""
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}