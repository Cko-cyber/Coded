package com.example.coded.data

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val participant1: String = "",
    val participant2: String = "",
    val listingId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp = Timestamp.now(),
    val unreadCount: Int = 0
)