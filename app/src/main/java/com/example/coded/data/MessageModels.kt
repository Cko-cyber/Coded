package com.example.coded.data

import com.google.firebase.Timestamp

// ============================================================
// ENHANCED MESSAGE SYSTEM DATA MODELS
// ============================================================

/**
 * Represents a conversation between two users
 */
data class Conversation(
    val id: String = "",
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

/**
 * Information about a conversation participant
 */
data class ParticipantInfo(
    val userId: String = "",
    val name: String = "",
    val profilePic: String = "",
    val phone: String = "",
    val lastSeen: Timestamp = Timestamp.now(),
    val isOnline: Boolean = false
)

/**
 * Context about which listing is being discussed
 */
data class ListingContext(
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImage: String = "",
    val listingPrice: Long = 0,
    val messageCount: Int = 0,
    val lastMessageTime: Timestamp = Timestamp.now()
)

/**
 * A message within a conversation
 */
data class ConversationMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val listingId: String? = null,
    val listingSnapshot: ListingSnapshot? = null,
    val type: String = "TEXT",
    val status: String = "SENT", // SENT, DELIVERED, READ
    val reactions: Map<String, String> = emptyMap(),
    val replyToId: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val editedAt: Timestamp? = null,
    val deletedFor: List<String> = emptyList(),
    val deliveredAt: Timestamp? = null,
    val readAt: Timestamp? = null
)

/**
 * Snapshot of listing info embedded in message
 */
data class ListingSnapshot(
    val title: String = "",
    val price: Long = 0,
    val image: String = "",
    val isActive: Boolean = true
)

/**
 * Message status enum
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED;

    fun toFirestoreString(): String = this.name

    companion object {
        fun fromString(value: String?): MessageStatus {
            return when (value?.uppercase()) {
                "SENDING" -> SENDING
                "SENT" -> SENT
                "DELIVERED" -> DELIVERED
                "READ" -> READ
                "FAILED" -> FAILED
                else -> SENT
            }
        }
    }
}