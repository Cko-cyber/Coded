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
            // Save message
            messagesCollection.document(message.id).set(message).await()

            // Update or create chat
            updateChat(message)

            println("✅ Message sent: ${message.id}")
            true
        } catch (e: Exception) {
            println("❌ Error sending message: ${e.message}")
            false
        }
    }

    private suspend fun updateChat(message: Message) {
        val chatId = generateChatId(message.senderId, message.receiverId, message.listingId)

        val chat = Chat(
            id = chatId,
            participant1 = message.senderId,
            participant2 = message.receiverId,
            listingId = message.listingId,
            lastMessage = message.content,
            lastMessageTime = message.createdAt,
            unreadCount = if (message.senderId != message.receiverId) 1 else 0
        )

        chatsCollection.document(chatId).set(chat).await()
    }

    fun getMessages(chatId: String): Flow<List<Message>> = flow {
        try {
            val query = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .orderBy("created_at", Query.Direction.ASCENDING)

            val snapshot = query.get().await()
            val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
            emit(messages)
        } catch (e: Exception) {
            println("❌ Error getting messages: ${e.message}")
            emit(emptyList())
        }
    }

    fun getUserChats(userId: String): Flow<List<Chat>> = flow {
        try {
            val query = chatsCollection
                .whereArrayContains("participants", userId)
                .orderBy("last_message_time", Query.Direction.DESCENDING)

            val snapshot = query.get().await()
            val chats = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
            emit(chats)
        } catch (e: Exception) {
            println("❌ Error getting user chats: ${e.message}")
            emit(emptyList())
        }
    }

    private fun generateChatId(user1: String, user2: String, listingId: String): String {
        val participants = listOf(user1, user2).sorted()
        return "${participants[0]}_${participants[1]}_$listingId"
    }
}