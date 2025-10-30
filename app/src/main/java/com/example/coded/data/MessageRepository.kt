package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageRepository {

    private val firestore = Firebase.firestore

    // ✅ Send message
    suspend fun sendMessage(message: Message): Boolean {
        return try {
            val messageData = mapOf(
                "id" to message.id,
                "listing_id" to message.listingId,
                "sender_id" to message.senderId,
                "receiver_id" to message.receiverId,
                "content" to message.content,
                "is_read" to message.isRead,
                "status" to message.status.name,
                "created_at" to message.createdAt,
                "chat_id" to message.chatId
            )

            firestore.collection("messages")
                .document(message.id)
                .set(messageData)
                .await()

            val chatData = mapOf(
                "id" to message.chatId,
                "participant1" to message.senderId,
                "participant2" to message.receiverId,
                "listing_id" to message.listingId,
                "last_message" to message.content,
                "last_message_time" to message.createdAt,
                "unread_count" to 0,
                "participants" to listOf(message.senderId, message.receiverId)
            )

            firestore.collection("chats")
                .document(message.chatId)
                .set(chatData)
                .await()

            println("✅ MessageRepository: Message sent successfully with ID: ${message.id}")
            true
        } catch (e: Exception) {
            println("❌ MessageRepository: Error sending message: ${e.message}")
            false
        }
    }

    // ✅ Get real-time messages safely
    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereEqualTo("chat_id", chatId)
            .orderBy("created_at")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("❌ MessageRepository: Error listening to messages: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            Message(
                                id = doc.id,
                                listingId = doc.getString("listing_id") ?: "",
                                senderId = doc.getString("sender_id") ?: "",
                                receiverId = doc.getString("receiver_id") ?: "",
                                content = doc.getString("content") ?: "",
                                isRead = doc.getBoolean("is_read") ?: false,
                                status = MessageStatus.valueOf(doc.getString("status") ?: "SENT"),
                                createdAt = doc.getTimestamp("created_at") ?: Timestamp.now(),
                                chatId = doc.getString("chat_id") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(messages.sortedBy { it.createdAt })
                }
            }

        awaitClose { listener.remove() }
    }

    // ✅ Get user chats safely
    fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("❌ MessageRepository: Error listening to chats: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        try {
                            Chat(
                                id = doc.getString("id") ?: "",
                                participant1 = doc.getString("participant1") ?: "",
                                participant2 = doc.getString("participant2") ?: "",
                                listingId = doc.getString("listing_id") ?: "",
                                lastMessage = doc.getString("last_message") ?: "",
                                lastMessageTime = doc.getTimestamp("last_message_time") ?: Timestamp.now(),
                                unreadCount = doc.getLong("unread_count")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(chats.sortedByDescending { it.lastMessageTime })
                }
            }

        awaitClose { listener.remove() }
    }

    // ✅ Mark all messages in a chat as read
    suspend fun markMessagesAsRead(chatId: String, userId: String) {
        try {
            val messages = firestore.collection("messages")
                .whereEqualTo("chat_id", chatId)
                .whereEqualTo("receiver_id", userId)
                .whereEqualTo("is_read", false)
                .get()
                .await()

            val batch = firestore.batch()

            messages.documents.forEach { doc ->
                val ref = firestore.collection("messages").document(doc.id)
                batch.update(ref, "is_read", true)
                batch.update(ref, "status", MessageStatus.READ.name)
            }

            batch.commit().await()
            println("✅ Updated ${messages.documents.size} messages as read")
        } catch (e: Exception) {
            println("❌ Error marking messages as read: ${e.message}")
        }
    }

    // ✅ Update message to DELIVERED
    suspend fun updateMessageToDelivered(messageId: String) {
        try {
            firestore.collection("messages")
                .document(messageId)
                .update("status", MessageStatus.DELIVERED.name)
                .await()
            println("✅ Message $messageId marked as DELIVERED")
        } catch (e: Exception) {
            println("❌ Error updating message to delivered: ${e.message}")
        }
    }

    // ✅ Update message to READ
    suspend fun updateMessageToRead(messageId: String) {
        try {
            firestore.collection("messages")
                .document(messageId)
                .update(
                    "status", MessageStatus.READ.name,
                    "is_read", true
                )
                .await()
            println("✅ Message $messageId marked as READ")
        } catch (e: Exception) {
            println("❌ Error updating message to read: ${e.message}")
        }
    }

    // ✅ Unified updateMessageStatus for ViewModel compatibility
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        try {
            val updateData = when (status) {
                MessageStatus.DELIVERED -> mapOf("status" to status.name)
                MessageStatus.READ -> mapOf(
                    "status" to status.name,
                    "is_read" to true
                )
                else -> mapOf("status" to status.name)
            }

            firestore.collection("messages")
                .document(messageId)
                .update(updateData)
                .await()

            println("✅ Message $messageId updated to ${status.name}")
        } catch (e: Exception) {
            println("❌ Error updating message status: ${e.message}")
        }
    }

    // ✅ Get user by ID
    suspend fun getUserById(userId: String): User? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) doc.toObject(User::class.java) else null
        } catch (e: Exception) {
            println("❌ Error getting user: ${e.message}")
            null
        }
    }
}
