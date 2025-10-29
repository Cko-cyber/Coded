package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Message(
    val id: String = "",

    // ✅ Use consistent field names - choose ONE naming convention
    @PropertyName("listing_id")
    val listingId: String = "",

    @PropertyName("sender_id")
    val senderId: String = "",

    @PropertyName("receiver_id")
    val receiverId: String = "",

    val content: String = "",

    @PropertyName("is_read")
    val isRead: Boolean = false,

    @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(),

    // ✅ CRITICAL: Add chat_id field
    @PropertyName("chat_id")
    val chatId: String = ""
)