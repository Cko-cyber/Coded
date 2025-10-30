package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantDetails: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val lastMessageListingId: String? = null,
    val unreadCount: Map<String, Int> = emptyMap(), // userId -> count
    val typingStatus: Map<String, Boolean> = emptyMap(), // userId -> isTyping
    val listingContexts: Map<String, ListingContext> = emptyMap(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)