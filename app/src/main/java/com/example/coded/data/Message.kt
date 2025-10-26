package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Message(
    val id: String = "",
    val listingId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",

    @PropertyName("is_read")
    val isRead: Boolean = false,

    @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now()
)

data class Chat(
    val id: String = "",
    val participant1: String = "",
    val participant2: String = "",
    val listingId: String = "",

    @PropertyName("last_message")
    val lastMessage: String = "",

    @PropertyName("last_message_time")
    val lastMessageTime: Timestamp = Timestamp.now(),

    @PropertyName("unread_count")
    val unreadCount: Int = 0
)