package com.example.coded.data.models

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "", // e.g. "user1_user2"
    val participants: List<String> = emptyList(),
    val participantDetails: Map<String, ParticipantInfo> = emptyMap(),
    val listingContexts: Map<String, ListingContext> = emptyMap(),

    val lastMessage: String = "",
    val lastMessageListingId: String? = null,
    val lastMessageTime: Timestamp = Timestamp.now(),

    val unreadCount: Map<String, Int> = emptyMap(),
    val typingStatus: Map<String, Boolean> = emptyMap(),

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class ParticipantInfo(
    val userId: String = "",
    val name: String = "",
    val profilePic: String = "",
    val phone: String = "",
    val lastSeen: Timestamp = Timestamp.now(),
    val isOnline: Boolean = false
)

data class ListingContext(
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImage: String = "",
    val listingPrice: Long = 0,
    val messageCount: Int = 0,
    val lastMessageTime: Timestamp = Timestamp.now()
)

data class ConversationMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",

    val listingId: String? = null,
    val listingSnapshot: ListingSnapshot? = null,

    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,

    val reactions: Map<String, String> = emptyMap(),
    val replyToId: String? = null,

    val createdAt: Timestamp = Timestamp.now(),
    val editedAt: Timestamp? = null,
    val deletedFor: List<String> = emptyList(),

    val deliveredAt: Timestamp? = null,
    val readAt: Timestamp? = null
)

data class ListingSnapshot(
    val title: String = "",
    val price: Long = 0,
    val image: String = "",
    val isActive: Boolean = true
)

enum class MessageType {
    TEXT, IMAGE, LISTING_INQUIRY, SYSTEM, QUICK_REPLY
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

data class MessageNotification(
    val id: String = "",
    val userId: String = "",
    val type: String = "new_message",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val listingId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)
