package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Chat(
    val id: String = "",

    @PropertyName("participant1")
    val participant1: String = "",

    @PropertyName("participant2")
    val participant2: String = "",

    @PropertyName("listing_id")
    val listingId: String = "",

    @PropertyName("last_message")
    val lastMessage: String = "",

    @PropertyName("last_message_time")
    val lastMessageTime: Timestamp = Timestamp.now(),

    @PropertyName("unread_count")
    val unreadCount: Int = 0,

    val participants: List<String> = emptyList()
)