package com.example.coded.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// --------------------------
// ENHANCED MESSAGE REPOSITORY
// --------------------------
class EnhancedMessageRepository {

    private val db = FirebaseFirestore.getInstance()
    private val conversationsRef = db.collection("conversations")
    private val notificationsRef = db.collection("notifications")
    private val usersRef = db.collection("users")
    private val TAG = "EnhancedMessageRepo"

    // REMOVED: Duplicate MessageStatus enum declaration

    // Convert enum to Firestore string
    fun MessageStatus.toFirestoreString(): String = this.name

    // Convert Firestore string back to enum
    fun String.toMessageStatus(): MessageStatus = MessageStatus.valueOf(this)

    // Add these methods to your EnhancedMessageRepository class
    suspend fun markMessagesAsDelivered(conversationId: String, userId: String) {
        // For now, we'll use the same implementation as markMessagesAsRead
        // You can customize this based on your delivery logic
        markMessagesAsRead(conversationId, userId)
    }

    fun listenToConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = conversationsRef
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull {
                    it.toObject(Conversation::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    fun listenToMessages(conversationId: String): Flow<List<ConversationMessage>> = callbackFlow {
        val listener = conversationsRef.document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(ConversationMessage::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    // ... rest of your repository methods remain the same
    // --------------------------
    // CONVERSATION MANAGEMENT
    // --------------------------
    suspend fun getOrCreateConversation(user1Id: String, user2Id: String): String {
        val conversationId = generateConversationId(user1Id, user2Id)
        try {
            val doc = conversationsRef.document(conversationId).get().await()
            if (!doc.exists()) {
                val user1Info = getUserInfo(user1Id)
                val user2Info = getUserInfo(user2Id)
                val conversation = Conversation(
                    id = conversationId,
                    participants = listOf(user1Id, user2Id),
                    participantDetails = mapOf(user1Id to user1Info, user2Id to user2Info),
                    unreadCount = mapOf(user1Id to 0, user2Id to 0),
                    typingStatus = mapOf(user1Id to false, user2Id to false),
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                conversationsRef.document(conversationId).set(conversation).await()
                Log.d(TAG, "✅ Created conversation $conversationId")
            } else Log.d(TAG, "📱 Using existing conversation $conversationId")
            return conversationId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating conversation", e)
            throw e
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

    // --------------------------
    // MESSAGES
    // --------------------------
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String,
        listingId: String? = null,
        listingSnapshot: ListingSnapshot? = null
    ): Boolean {
        return try {
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

            conversationsRef.document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            updateConversationAfterMessage(conversationId, message, receiverId)
            createMessageNotification(conversationId, senderId, receiverId, content, listingId)

            Log.d(TAG, "✅ Message sent")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending message", e)
            false
        }
    }

    private suspend fun updateConversationAfterMessage(
        conversationId: String,
        message: ConversationMessage,
        receiverId: String
    ) {
        try {
            val updates = hashMapOf<String, Any>(
                "lastMessage" to message.content,
                "lastMessageTime" to message.createdAt,
                "updatedAt" to Timestamp.now(),
                "unreadCount.$receiverId" to FieldValue.increment(1)
            )

            if (message.listingId != null && message.listingSnapshot != null) {
                updates["lastMessageListingId"] = message.listingId
                updates["listingContexts.${message.listingId}"] = mapOf(
                    "listingId" to message.listingId,
                    "listingTitle" to message.listingSnapshot.title,
                    "listingImage" to message.listingSnapshot.image,
                    "listingPrice" to message.listingSnapshot.price,
                    "messageCount" to FieldValue.increment(1),
                    "lastMessageTime" to message.createdAt
                )
            }

            conversationsRef.document(conversationId).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating conversation", e)
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ConversationMessage>> = callbackFlow {
        val listener = conversationsRef.document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(ConversationMessage::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = conversationsRef
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val convos = snapshot?.documents?.mapNotNull {
                    it.toObject(Conversation::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(convos)
            }
        awaitClose { listener.remove() }
    }

    // --------------------------
    // READ / DELIVERY / TYPING
    // --------------------------
    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
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

            val batch = db.batch()
            messages.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "status" to MessageStatus.READ.toFirestoreString(),
                    "readAt" to Timestamp.now()
                ))
            }
            batch.commit().await()
            conversationsRef.document(conversationId).update("unreadCount.$userId", 0).await()
        } catch (e: Exception) { Log.e(TAG, "❌ Error marking messages read", e) }
    }

    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        try { conversationsRef.document(conversationId).update("typingStatus.$userId", isTyping).await() }
        catch (e: Exception) { Log.e(TAG, "❌ Error updating typing status", e) }
    }

    suspend fun updateMessageStatus(conversationId: String, messageId: String, status: MessageStatus) {
        try {
            conversationsRef.document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("status", status.toFirestoreString())
                .await()
        } catch (e: Exception) { Log.e(TAG, "❌ Error updating message status", e) }
    }

    // --------------------------
    // REACTIONS / DELETE
    // --------------------------
    suspend fun addReaction(conversationId: String, messageId: String, userId: String, emoji: String): Boolean {
        return try {
            conversationsRef.document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("reactions.$userId", emoji)
                .await()
            true
        } catch (e: Exception) { Log.e(TAG, "❌ Error adding reaction", e); false }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String, userId: String): Boolean {
        return try {
            conversationsRef.document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("deletedFor", FieldValue.arrayUnion(userId))
                .await()
            true
        } catch (e: Exception) { Log.e(TAG, "❌ Error deleting message", e); false }
    }

    // --------------------------
    // NOTIFICATIONS
    // --------------------------
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
        } catch (e: Exception) { Log.e(TAG, "❌ Error creating notification", e) }
    }

    // --------------------------
    // UTILITIES
    // --------------------------
    private fun generateConversationId(user1Id: String, user2Id: String): String =
        listOf(user1Id, user2Id).sorted().joinToString("_")

    fun getQuickReplies(): List<String> = listOf(
        "Hello! I'm interested in this item",
        "Is this still available?",
        "What's your best price?",
        "Can I come view it?",
        "When can we meet?"
    )

    // --------------------------
    // Typing observer
    // --------------------------
    fun observeTyping(conversationId: String, userId: String): Flow<Boolean> = callbackFlow {
        val listener = conversationsRef.document(conversationId).addSnapshotListener { doc, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val typingMap = doc?.get("typingStatus") as? Map<String, Boolean> ?: emptyMap()
            trySend(typingMap[userId] ?: false)
        }
        awaitClose { listener.remove() }
    }
}