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

            println("📤 Sending message:")
            println("   Chat ID: $chatId")
            println("   From: ${message.senderId} → To: ${message.receiverId}")
            println("   Listing: ${message.listingId}")
            println("   Content: ${message.content}")

            // Save message with chat_id
            messagesCollection.document(message.id).set(messageWithChatId).await()

            // Update or create chat
            updateChat(messageWithChatId, chatId)

            println("✅ Message sent successfully!")
            true
        } catch (e: Exception) {
            println("❌ Error sending message: ${e.message}")
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
                participants = listOf(message.senderId, message.receiverId) // ✅ Required for querying
            )

            println("💾 Updating chat: $chatId")
            chatsCollection.document(chatId).set(chat).await()
            println("✅ Chat updated successfully")
        } catch (e: Exception) {
            println("❌ Error updating chat: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = flow {
        try {
            println("📥 Fetching messages for chat: $chatId")

            val query = messagesCollection
                .whereEqualTo("chat_id", chatId) // ✅ Now this field exists
                .orderBy("created_at", Query.Direction.ASCENDING)

            val snapshot = query.get().await()
            val messages = snapshot.documents.mapNotNull {
                it.toObject(Message::class.java)
            }

            println("✅ Retrieved ${messages.size} messages")
            emit(messages)
        } catch (e: Exception) {
            println("❌ Error getting messages: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun getUserChats(userId: String): Flow<List<Chat>> = flow {
        try {
            println("📥 Fetching chats for user: $userId")

            // ✅ Query using participants array
            val query = chatsCollection
                .whereArrayContains("participants", userId)
                .orderBy("last_message_time", Query.Direction.DESCENDING)

            val snapshot = query.get().await()
            val chats = snapshot.documents.mapNotNull {
                it.toObject(Chat::class.java)
            }

            println("✅ Retrieved ${chats.size} chats")
            emit(chats)
        } catch (e: Exception) {
            println("❌ Error getting user chats: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    private fun generateChatId(user1: String, user2: String, listingId: String): String {
        val participants = listOf(user1, user2).sorted()
        return "${participants[0]}_${participants[1]}_$listingId"
    }

    // Mark messages as read when user opens a chat
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
            println("❌ Error marking messages as read: ${e.message}")
        }
    }
}