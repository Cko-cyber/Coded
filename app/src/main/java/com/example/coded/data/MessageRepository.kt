package com.example.coded.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class MessageRepository {
    private val db = Firebase.firestore
    private val messagesCollection = db.collection("messages")
    private val chatsCollection = db.collection("chats")

    suspend fun sendMessage(message: Message): Boolean {
        return try {
            // ✅ Generate chat ID and include it in the message
            val chatId = generateChatId(message.senderId, message.receiverId, message.listingId)
            val messageWithChatId = message.copy(chatId = chatId)

            println("📤 [MessageRepository] Starting message send...")
            println("   Chat ID: $chatId")
            println("   From: ${message.senderId} → To: ${message.receiverId}")
            println("   Listing: ${message.listingId}")
            println("   Content: ${message.content}")
            println("   Message ID: ${message.id}")

            // Test Firestore connection first
            println("🔍 [MessageRepository] Testing Firestore connection...")
            val testResult = testFirestoreConnection()
            if (!testResult) {
                println("❌ [MessageRepository] Firestore connection test failed!")
                return false
            }

            // Save message with chat_id
            println("💾 [MessageRepository] Saving message to Firestore...")
            messagesCollection.document(message.id).set(messageWithChatId).await()
            println("✅ [MessageRepository] Message saved successfully!")

            // Update or create chat
            updateChat(messageWithChatId, chatId)

            println("✅ [MessageRepository] Message sent successfully!")
            true
        } catch (e: Exception) {
            println("❌ [MessageRepository] Error sending message: ${e.message}")
            println("❌ [MessageRepository] Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun testFirestoreConnection(): Boolean {
        return try {
            println("🔍 [MessageRepository] Testing Firestore write...")
            val testDoc = db.collection("connection_test").document("test")
            testDoc.set(mapOf(
                "timestamp" to Timestamp.now(),
                "test" to "connection_test"
            )).await()
            println("✅ [MessageRepository] Firestore write test successful")

            // Clean up test document
            testDoc.delete().await()
            println("✅ [MessageRepository] Test document cleaned up")
            true
        } catch (e: Exception) {
            println("❌ [MessageRepository] Firestore connection test failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun updateChat(message: Message, chatId: String) {
        try {
            val chat = Chat(
                id = chatId,
                participant1 = message.senderId,
                participant2 = message.receiverId,
                listingId = message.listingId,
                lastMessage = message.content,
                lastMessageTime = message.createdAt,
                unreadCount = if (message.senderId != message.receiverId) 1 else 0,
                participants = listOf(message.senderId, message.receiverId)
            )

            println("💾 [MessageRepository] Updating chat: $chatId")
            chatsCollection.document(chatId).set(chat).await()
            println("✅ [MessageRepository] Chat updated successfully")
        } catch (e: Exception) {
            println("❌ [MessageRepository] Error updating chat: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = flow {
        try {
            println("📥 [MessageRepository] Fetching messages for chat: $chatId")

            val query = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .orderBy("created_at", Query.Direction.ASCENDING)

            val snapshot = query.get().await()
            val messages = snapshot.documents.mapNotNull {
                it.toObject(Message::class.java)
            }

            println("✅ [MessageRepository] Retrieved ${messages.size} messages")
            emit(messages)
        } catch (e: Exception) {
            println("❌ [MessageRepository] Error getting messages: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun getUserChats(userId: String): Flow<List<Chat>> = flow {
        try {
            println("📥 [MessageRepository] Fetching chats for user: $userId")

            // Try both query methods
            val query = chatsCollection
                .whereArrayContains("participants", userId)
                .orderBy("last_message_time", Query.Direction.DESCENDING)

            val snapshot = query.get().await()
            val chats = snapshot.documents.mapNotNull {
                it.toObject(Chat::class.java)
            }

            println("✅ [MessageRepository] Retrieved ${chats.size} chats")
            emit(chats)
        } catch (e: Exception) {
            println("❌ [MessageRepository] Error getting user chats: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    private fun generateChatId(user1: String, user2: String, listingId: String): String {
        val participants = listOf(user1, user2).sorted()
        return "${participants[0]}_${participants[1]}_$listingId"
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String) {
        try {
            val messages = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .whereEqualTo("receiver_id", userId)
                .whereEqualTo("is_read", false)
                .get()
                .await()

            val batch = db.batch()
            messages.documents.forEach { doc ->
                batch.update(doc.reference, "is_read", true)
            }
            batch.commit().await()

            // Reset unread count for this chat
            chatsCollection.document(chatId).update("unread_count", 0).await()
        } catch (e: Exception) {
            println("❌ [MessageRepository] Error marking messages as read: ${e.message}")
        }
    }
}