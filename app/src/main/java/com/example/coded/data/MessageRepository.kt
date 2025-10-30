package com.example.coded.data

import android.R
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class EnhancedMessageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val conversationsRef = db.collection("conversations")
    private val notificationsRef = db.collection("notifications")
    private val usersRef = db.collection("users")
    private val TAG = "EnhancedMessageRepo"

    // Cache for user info to reduce Firestore reads
    private val userInfoCache = mutableMapOf<String, ParticipantInfo>()
    private val conversationCache = mutableMapOf<String, Conversation>()

    // Extension functions for MessageStatus
    private fun MessageStatus.toFirestoreString(): String = this.name

    private fun String.toMessageStatus(): MessageStatus = MessageStatus.valueOf(this)

    // ============================================================
    // CONVERSATION LISTENERS (KEEP THESE METHODS!)
    // ============================================================

    /**
     * Listen to user's conversations in real-time
     */
    fun listenToConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        Log.d(TAG, "🔍 Setting up conversation listener for user: $userId")

        val listener = conversationsRef
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to conversations", error)
                    close(error)
                    return@addSnapshotListener
                }

                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val conversation = doc.toObject(Conversation::class.java)
                        conversation?.copy(id = doc.id)?.also {
                            // Cache the conversation
                            conversationCache[doc.id] = it
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing conversation", e)
                        null
                    }
                } ?: emptyList()

                trySend(conversations)
            }

        awaitClose {
            Log.d(TAG, "🛑 Removing conversation listener")
            listener.remove()
        }
    }

    /**
     * Listen to messages in a conversation in real-time
     */
    fun listenToMessages(conversationId: String): Flow<List<ConversationMessage>> = callbackFlow {
        Log.d(TAG, "🔍 Setting up message listener for conversation: $conversationId")

        val listener = conversationsRef
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to messages", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ConversationMessage::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose {
            Log.d(TAG, "🛑 Removing message listener")
            listener.remove()
        }
    }

    // ============================================================
    // CONVERSATION MANAGEMENT - FIXED VERSION
    // ============================================================

    /**
     * Get or create a conversation between two users - FIXED VERSION
     * Uses Try-Create-First approach to avoid permission denied errors
     */
    suspend fun getOrCreateConversation(
        user1Id: String,
        user2Id: String
    ): String {
        val conversationId = generateConversationId(user1Id, user2Id)

        try {
            Log.d(TAG, "🔍 Attempting to create conversation: $conversationId")

            // ✅ FIXED: Try to create first, handle if it already exists
            try {
                // Get user info
                val user1Info = getUserInfo(user1Id)
                val user2Info = getUserInfo(user2Id)

                // Create new conversation - SIMPLIFIED VERSION
                val conversation = hashMapOf<String, Any>(
                    "id" to conversationId,
                    "participants" to listOf(user1Id, user2Id),
                    "participantDetails" to mapOf(
                        user1Id to mapOf(
                            "userId" to user1Info.userId,
                            "name" to user1Info.name,
                            "profilePic" to user1Info.profilePic,
                            "phone" to user1Info.phone,
                            "isOnline" to user1Info.isOnline
                        ),
                        user2Id to mapOf(
                            "userId" to user2Info.userId,
                            "name" to user2Info.name,
                            "profilePic" to user2Info.profilePic,
                            "phone" to user2Info.phone,
                            "isOnline" to user2Info.isOnline
                        )
                    ),
                    "listingContexts" to emptyMap<String, Any>(),
                    "lastMessage" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "unreadCount" to mapOf(
                        user1Id to 0,
                        user2Id to 0
                    ),
                    "typingStatus" to mapOf(
                        user1Id to false,
                        user2Id to false
                    ),
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )

                Log.d(TAG, "📤 Creating conversation document...")
                conversationsRef.document(conversationId).set(conversation).await()
                Log.d(TAG, "✅ Successfully created new conversation: $conversationId")

                return conversationId

            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                    Log.d(TAG, "📱 Conversation already exists: $conversationId")
                    return conversationId
                } else {
                    Log.w(TAG, "⚠️ Other error creating conversation, trying to read: ${e.message}")
                    return tryReadExistingConversation(conversationId, user1Id, user2Id)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error in getOrCreateConversation", e)
            throw e
        }
    }

    /**
     * Fallback method to read existing conversation when creation fails
     */
    private suspend fun tryReadExistingConversation(
        conversationId: String,
        user1Id: String,
        user2Id: String
    ): String {
        return try {
            Log.d(TAG, "🔄 Attempting to read existing conversation: $conversationId")
            val doc = conversationsRef.document(conversationId).get().await()

            if (doc.exists()) {
                Log.d(TAG, "📱 Using existing conversation: $conversationId")
                conversationId
            } else {
                Log.e(TAG, "❌ Conversation doesn't exist and couldn't be created")
                throw Exception("Failed to create or find conversation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading existing conversation", e)
            throw e
        }
    }

    /**
     * Get user info from Firestore
     */
    private suspend fun getUserInfo(userId: String): ParticipantInfo {
        return try {
            val doc = usersRef.document(userId).get().await()
            ParticipantInfo(
                userId = userId,
                name = doc.getString("full_name") ?: "User",
                profilePic = doc.getString("profile_pic") ?: "",
                phone = doc.getString("mobile_number") ?: "",
                lastSeen = doc.getTimestamp("last_active") ?: Timestamp.now(),
                isOnline = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user info", e)
            ParticipantInfo(userId = userId, name = "User")
        }
    }

    /**
     * Send a message in a conversation
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String,
        listingId: String? = null,
        listingSnapshot: ListingSnapshot? = null
    ): Boolean {
        return try {
            val messageId = conversationsRef
                .document(conversationId)
                .collection("messages")
                .document().id

            val message = ConversationMessage(
                id = messageId,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                listingId = listingId,
                listingSnapshot = listingSnapshot,
                type = "TEXT",
                status = MessageStatus.SENT.toFirestoreString(),
                createdAt = Timestamp.now()
            )

            // Add message to subcollection
            conversationsRef
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            // Update conversation metadata
            updateConversationAfterMessage(
                conversationId = conversationId,
                message = message,
                receiverId = receiverId
            )

            // Create notification
            createMessageNotification(
                conversationId = conversationId,
                senderId = senderId,
                receiverId = receiverId,
                message = content,
                listingId = listingId
            )

            Log.d(TAG, "✅ Message sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending message", e)
            false
        }
    }

    /**
     * Update conversation after sending a message - FIXED VERSION
     */
    private suspend fun updateConversationAfterMessage(
        conversationId: String,
        message: ConversationMessage,
        receiverId: String
    ) {
        try {
            // ✅ FIXED: Use simple map with explicit typing
            val updates = mapOf<String, Any>(
                "lastMessage" to message.content,
                "lastMessageTime" to (message.createdAt ?: Timestamp.now()),
                "updatedAt" to Timestamp.now(),
                "unreadCount.$receiverId" to FieldValue.increment(1)
            )

            conversationsRef.document(conversationId).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating conversation", e)
        }
    }

    // ============================================================
    // READ RECEIPTS & STATUS
    // ============================================================

    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
        try {
            val messages = conversationsRef
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("receiverId", userId)
                .whereIn("status", listOf("SENT", "DELIVERED"))
                .get()
                .await()

            val batch = db.batch()
            messages.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "status" to MessageStatus.READ.toFirestoreString(),
                    "readAt" to Timestamp.now()
                ))
            }
            batch.commit().await()

            conversationsRef
                .document(conversationId)
                .update("unreadCount.$userId", 0)
                .await()

            Log.d(TAG, "✅ Marked ${messages.size()} messages as read")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking messages as read", e)
        }
    }

    suspend fun markMessagesAsDelivered(conversationId: String, receiverId: String) {
        try {
            val messages = conversationsRef
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("status", "SENT")
                .get()
                .await()

            val batch = db.batch()
            messages.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "status" to MessageStatus.DELIVERED.toFirestoreString(),
                    "deliveredAt" to Timestamp.now()
                ))
            }
            batch.commit().await()

            Log.d(TAG, "✅ Marked ${messages.size()} messages as delivered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking messages as delivered", e)
        }
    }

    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        try {
            conversationsRef
                .document(conversationId)
                .update("typingStatus.$userId", isTyping)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating typing status", e)
        }
    }

    suspend fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus
    ) {
        try {
            conversationsRef
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("status", status.toFirestoreString())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating message status", e)
        }
    }

    // ============================================================
    // MESSAGE INTERACTIONS
    // ============================================================

    suspend fun addReaction(
        conversationId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Boolean {
        return try {
            conversationsRef
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("reactions.$userId", emoji)
                .await()

            Log.d(TAG, "✅ Added reaction: $emoji")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding reaction", e)
            false
        }
    }

    suspend fun deleteMessage(
        conversationId: String,
        messageId: String,
        userId: String
    ): Boolean {
        return try {
            conversationsRef
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("deletedFor", FieldValue.arrayUnion(userId))
                .await()

            Log.d(TAG, "✅ Message deleted for user")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting message", e)
            false
        }
    }

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    private suspend fun createMessageNotification(
        conversationId: String,
        senderId: String,
        receiverId: String,
        message: String,
        listingId: String?
    ) {
        try {
            val senderDoc = usersRef.document(senderId).get().await()
            val senderName = senderDoc.getString("full_name") ?: "Someone"

            val notification = mapOf(
                "id" to notificationsRef.document().id,
                "userId" to receiverId,
                "type" to "new_message",
                "conversationId" to conversationId,
                "senderId" to senderId,
                "senderName" to senderName,
                "message" to message.take(100),
                "listingId" to listingId,
                "isRead" to false,
                "createdAt" to Timestamp.now()
            )

            notificationsRef.document(notification["id"] as String).set(notification).await()
            Log.d(TAG, "✅ Notification created")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating notification", e)
        }
    }

    // ============================================================
    // CACHE MANAGEMENT
    // ============================================================

    /**
     * Clear user cache
     */
    fun clearUserCache() {
        userInfoCache.clear()
        Log.d(TAG, "🧹 Cleared user cache")
    }

    /**
     * Clear conversation cache
     */
    fun clearConversationCache() {
        conversationCache.clear()
        Log.d(TAG, "🧹 Cleared conversation cache")
    }

    /**
     * Clear all cache
     */
    fun clearAllCache() {
        userInfoCache.clear()
        conversationCache.clear()
        Log.d(TAG, "🧹 Cleared all cache")
    }

    /**
     * Get cached conversation
     */
    fun getCachedConversation(conversationId: String): Conversation? {
        return conversationCache[conversationId]
    }

    /**
     * Get cached user info
     */
    fun getCachedUserInfo(userId: String): ParticipantInfo? {
        return userInfoCache[userId]
    }

    // ============================================================
    // UTILITY FUNCTIONS
    // ============================================================

    private fun generateConversationId(user1Id: String, user2Id: String): String {
        val sorted = listOf(user1Id, user2Id).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    fun getQuickReplies(): List<String> {
        return listOf(
            "Hello! I'm interested in this item",
            "Is this still available?",
            "What's your best price?",
            "Can I come view it?",
            "When can we meet?"
        )
    }

    // ============================================================
    // ALIAS METHODS FOR BACKWARD COMPATIBILITY
    // ============================================================

    /**
     * Alias for listenToMessages
     */
    fun observeMessages(conversationId: String): Flow<List<ConversationMessage>> =
        listenToMessages(conversationId)

    /**
     * Alias for listenToConversations
     */
    fun observeUserConversations(userId: String): Flow<List<Conversation>> =
        listenToConversations(userId)
}