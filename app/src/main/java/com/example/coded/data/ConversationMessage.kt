package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ConversationMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val listingId: String? = null,
    val listingSnapshot: ListingSnapshot? = null,
    val type: String = "TEXT", // TEXT, IMAGE, SYSTEM
    val status: String = MessageStatus.SENT.name, // SENT, DELIVERED, READ
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val deletedFor: List<String> = emptyList(), // userIds that deleted this message
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val readAt: Timestamp? = null
)