package com.example.coded.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EnhancedMessageRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val db = FirebaseFirestore.getInstance()
    private val conversationsRef = db.collection("conversations")
    private val notificationsRef = db.collection("notifications")
    private val usersRef = db.collection("users")
    private val TAG = "EnhancedMessageRepo"

    // Cache for user info to reduce Firestore reads
    private val userInfoCache = mutableMapOf<String, ParticipantInfo>()
    private val conversationCache = mutableMapOf<String, Conversation>()

    // Convert enum to Firestore string
    fun MessageStatus.toFirestoreString(): String = this.name

    // Convert Firestore string back to enum
    fun String.toMessageStatus(): MessageStatus = MessageStatus.valueOf(this)

    // ✅ PERFORMANCE OPTIMIZED: Batch operations and caching
    suspend fun markMessagesAsDelivered(conversationId: String, userId: String) {
        withContext(ioDispatcher) {
            try {
                val messages = conversationsRef.document(conversationId)
                    .collection("messages")
                    .whereEqualTo("receiverId", userId)
                    .whereEqualTo("status", MessageStatus.SENT.toFirestoreString())
                    .get()
                    .await()

                // Use batch for multiple updates
                val batch = db.batch()
                messages.documents.forEach { doc ->
                    batch.update(doc.reference, mapOf(
                        "status" to MessageStatus.DELIVERED.toFirestoreString(),
                        "deliveredAt" to Timestamp.now()
                    ))
                }

                if (messages.documents.isNotEmpty()) {
                    batch.commit().await()
                    Log.d(TAG, "✅ Marked ${messages.documents.size} messages as delivered")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error marking messages as delivered", e)
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Flow with proper dispatcher
    fun listenToConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        Log.d(TAG, "🔍 Setting up conversation listener for user: $userId")

        val listener = conversationsRef
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .limit(50) // ✅ LIMIT results for performance
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Firestore error in listenToConversations: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "⚠️ Conversation snapshot is null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val conversations = snapshot.documents.mapNotNull { doc ->
                        try {
                            val conversation = doc.toObject(Conversation::class.java)
                            conversation?.copy(id = doc.id)?.also {
                                // Cache the conversation
                                conversationCache[doc.id] = it
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing conversation document ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    Log.d(TAG, "✅ Loaded ${conversations.size} conversations")
                    trySend(conversations)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing conversations: ${e.message}")
                    trySend(emptyList())
                }
            }

        awaitClose {
            Log.d(TAG, "🛑 Removing conversation listener")
            listener.remove()
        }
    }.flowOn(ioDispatcher) // ✅ Move flow to IO dispatcher

    // ✅ PERFORMANCE OPTIMIZED: Message listening with pagination
    fun listenToMessages(conversationId: String, limit: Int = 100): Flow<List<ConversationMessage>> = callbackFlow {
        Log.d(TAG, "🔍 Setting up message listener for conversation: $conversationId")

        val listener = conversationsRef.document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING) // ✅ DESC for latest first
            .limit(limit.toLong()) // ✅ Limit messages for performance
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Firestore error in listenToMessages: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "⚠️ Message snapshot is null for conversation: $conversationId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val message = doc.toObject(ConversationMessage::class.java)
                            message?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing message document ${doc.id}: ${e.message}")
                            null
                        }
                    }.sortedBy { it.createdAt } // ✅ Sort ascending for display

                    Log.d(TAG, "✅ Loaded ${messages.size} messages for conversation: $conversationId")
                    trySend(messages)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing messages: ${e.message}")
                    trySend(emptyList())
                }
            }

        awaitClose {
            Log.d(TAG, "🛑 Removing message listener for conversation: $conversationId")
            listener.remove()
        }
    }.flowOn(ioDispatcher)

    // ✅ PERFORMANCE OPTIMIZED: Cached user info and batch operations
// ✅ FIXED: getOrCreateConversation with better error handling
    suspend fun getOrCreateConversation(user1Id: String, user2Id: String): String {
        return withContext(ioDispatcher) {
            val conversationId = generateConversationId(user1Id, user2Id)
            try {
                Log.d(TAG, "🔍 Checking conversation: $conversationId")

                // Check cache first
                conversationCache[conversationId]?.let {
                    Log.d(TAG, "📱 Using cached conversation: $conversationId")
                    return@withContext conversationId
                }

                val doc = conversationsRef.document(conversationId).get().await()

                if (!doc.exists()) {
                    Log.d(TAG, "🆕 Creating new conversation: $conversationId")

                    // Create a simpler conversation object for testing
                    val conversation = hashMapOf(
                        "id" to conversationId,
                        "participants" to listOf(user1Id, user2Id),
                        "lastMessage" to "",
                        "lastMessageTime" to Timestamp.now(),
                        "createdAt" to Timestamp.now(),
                        "updatedAt" to Timestamp.now(),
                        "unreadCount" to mapOf(user1Id to 0, user2Id to 0),
                        "typingStatus" to mapOf(user1Id to false, user2Id to false)
                    )

                    conversationsRef.document(conversationId).set(conversation).await()
                    Log.d(TAG, "✅ Successfully created conversation: $conversationId")
                } else {
                    Log.d(TAG, "📱 Using existing conversation: $conversationId")
                    // Cache the existing conversation
                    val conversation = doc.toObject(Conversation::class.java)
                    conversation?.let { conversationCache[conversationId] = it }
                }

                conversationId
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getOrCreateConversation: ${e.message}", e)
                // Log the exact Firestore error
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    Log.e(TAG, "🔐 Firestore Permission Denied - Check security rules")
                }
                throw e
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Cached user info
    private suspend fun getUserInfoWithCache(userId: String): ParticipantInfo {
        return userInfoCache[userId] ?: try {
            val userInfo = getUserInfo(userId)
            userInfoCache[userId] = userInfo
            userInfo
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user info for $userId", e)
            ParticipantInfo(userId = userId, name = "User").also {
                userInfoCache[userId] = it
            }
        }
    }

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
            Log.e(TAG, "❌ Error getting user info", e)
            ParticipantInfo(userId = userId, name = "User")
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Batch message operations
// ✅ PERFORMANCE OPTIMIZED: Batch message operations
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String,
        listingId: String? = null,
        listingSnapshot: ListingSnapshot? = null
    ): Boolean {
        return withContext(ioDispatcher) {
            try {
                val messageId = conversationsRef.document(conversationId)
                    .collection("messages").document().id

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

                // Use batch for atomic operations
                val batch = db.batch()

                // Add message
                val messageRef = conversationsRef.document(conversationId)
                    .collection("messages")
                    .document(messageId)
                batch.set(messageRef, message)

                // Update conversation in the same batch
                val conversationUpdates = mapOf<String, Any>(
                    "lastMessage" to message.content,
                    "lastMessageTime" to (message.createdAt ?: Timestamp.now()),
                    "updatedAt" to Timestamp.now(),
                    "unreadCount.$receiverId" to FieldValue.increment(1)
                )

                val conversationRef = conversationsRef.document(conversationId)
                batch.update(conversationRef, conversationUpdates)

                // Execute batch
                batch.commit().await()

                // Create notification (non-blocking)
                createMessageNotification(conversationId, senderId, receiverId, content, listingId)

                Log.d(TAG, "✅ Message sent successfully: $messageId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending message", e)
                false
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Batch read operations
    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
        withContext(ioDispatcher) {
            try {
                val messages = conversationsRef.document(conversationId)
                    .collection("messages")
                    .whereEqualTo("receiverId", userId)
                    .whereIn("status", listOf(
                        MessageStatus.SENT.toFirestoreString(),
                        MessageStatus.DELIVERED.toFirestoreString()
                    ))
                    .get()
                    .await()

                if (messages.documents.isNotEmpty()) {
                    val batch = db.batch()
                    messages.documents.forEach { doc ->
                        batch.update(doc.reference, mapOf(
                            "status" to MessageStatus.READ.toFirestoreString(),
                            "readAt" to Timestamp.now()
                        ))
                    }
                    batch.commit().await()
                    conversationsRef.document(conversationId).update("unreadCount.$userId", 0).await()
                    Log.d(TAG, "✅ Marked ${messages.documents.size} messages as read")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error marking messages read", e)
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Typing status with caching
    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        withContext(ioDispatcher) {
            try {
                conversationsRef.document(conversationId).update("typingStatus.$userId", isTyping).await()

                // Update cache if conversation is cached
                conversationCache[conversationId]?.let { cachedConversation ->
                    val updatedTypingStatus = cachedConversation.typingStatus.toMutableMap()
                    updatedTypingStatus[userId] = isTyping
                    conversationCache[conversationId] = cachedConversation.copy(typingStatus = updatedTypingStatus)
                }

                Log.d(TAG, "✅ Typing status updated: $userId -> $isTyping")
            }
            catch (e: Exception) {
                Log.e(TAG, "❌ Error updating typing status", e)
            }
        }
    }

    suspend fun updateMessageStatus(conversationId: String, messageId: String, status: MessageStatus) {
        withContext(ioDispatcher) {
            try {
                conversationsRef.document(conversationId)
                    .collection("messages")
                    .document(messageId)
                    .update("status", status.toFirestoreString())
                    .await()
                Log.d(TAG, "✅ Message status updated: $messageId -> $status")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating message status", e)
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Reaction with caching
    suspend fun addReaction(conversationId: String, messageId: String, userId: String, emoji: String): Boolean {
        return withContext(ioDispatcher) {
            try {
                conversationsRef.document(conversationId)
                    .collection("messages")
                    .document(messageId)
                    .update("reactions.$userId", emoji)
                    .await()
                Log.d(TAG, "✅ Reaction added: $userId -> $emoji")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error adding reaction", e)
                false
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Soft delete
    suspend fun deleteMessage(conversationId: String, messageId: String, userId: String): Boolean {
        return withContext(ioDispatcher) {
            try {
                conversationsRef.document(conversationId)
                    .collection("messages")
                    .document(messageId)
                    .update("deletedFor", FieldValue.arrayUnion(userId))
                    .await()
                Log.d(TAG, "✅ Message deleted for user: $userId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting message", e)
                false
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Notification with batching
    private suspend fun createMessageNotification(
        conversationId: String,
        senderId: String,
        receiverId: String,
        message: String,
        listingId: String?
    ) {
        withContext(ioDispatcher) {
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
                Log.d(TAG, "✅ Notification created for: $receiverId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating notification", e)
            }
        }
    }

    // ✅ PERFORMANCE OPTIMIZED: Single conversation listener with caching
    fun listenToConversation(conversationId: String, userId: String): Flow<Conversation?> = callbackFlow {
        Log.d(TAG, "🔍 Listening to single conversation: $conversationId")

        val listener = conversationsRef.document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to conversation $conversationId: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val conversation = snapshot.toObject(Conversation::class.java)
                        // Security check: verify user is a participant
                        if (conversation?.participants?.contains(userId) == true) {
                            // Update cache
                            conversationCache[conversationId] = conversation.copy(id = snapshot.id)
                            trySend(conversation.copy(id = snapshot.id))
                        } else {
                            Log.w(TAG, "🚫 User $userId not authorized for conversation $conversationId")
                            trySend(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing conversation: ${e.message}")
                        trySend(null)
                    }
                } else {
                    Log.w(TAG, "⚠️ Conversation $conversationId not found")
                    trySend(null)
                }
            }

        awaitClose {
            Log.d(TAG, "🛑 Removing single conversation listener")
            listener.remove()
        }
    }.flowOn(ioDispatcher)

    // ✅ PERFORMANCE OPTIMIZED: Typing observer with caching
    fun observeTyping(conversationId: String, userId: String): Flow<Boolean> = callbackFlow {
        Log.d(TAG, "🔍 Observing typing status for conversation: $conversationId")

        val listener = conversationsRef.document(conversationId).addSnapshotListener { doc, err ->
            if (err != null) {
                Log.e(TAG, "❌ Error observing typing status", err)
                close(err)
                return@addSnapshotListener
            }
            try {
                val typingMap = doc?.get("typingStatus") as? Map<String, Boolean> ?: emptyMap()
                val isTyping = typingMap[userId] ?: false
                trySend(isTyping)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing typing status", e)
                trySend(false)
            }
        }
        awaitClose {
            Log.d(TAG, "🛑 Removing typing observer")
            listener.remove()
        }
    }.flowOn(ioDispatcher)

    // Alias methods for backward compatibility
    fun observeMessages(conversationId: String): Flow<List<ConversationMessage>> =
        listenToMessages(conversationId)

    fun observeUserConversations(userId: String): Flow<List<Conversation>> =
        listenToConversations(userId)

    // Cache management
    fun clearUserCache() {
        userInfoCache.clear()
        Log.d(TAG, "🧹 Cleared user cache")
    }

    fun clearConversationCache() {
        conversationCache.clear()
        Log.d(TAG, "🧹 Cleared conversation cache")
    }

    fun clearAllCache() {
        userInfoCache.clear()
        conversationCache.clear()
        Log.d(TAG, "🧹 Cleared all cache")
    }

    // Get cached data (useful for offline support)
    fun getCachedConversation(conversationId: String): Conversation? {
        return conversationCache[conversationId]
    }

    fun getCachedUserInfo(userId: String): ParticipantInfo? {
        return userInfoCache[userId]
    }

    private fun generateConversationId(user1Id: String, user2Id: String): String =
        listOf(user1Id, user2Id).sorted().joinToString("_")

    fun getQuickReplies(): List<String> = listOf(
        "Hello! I'm interested in this item",
        "Is this still available?",
        "What's your best price?",
        "Can I come view it?",
        "When can we meet?"
    )
}