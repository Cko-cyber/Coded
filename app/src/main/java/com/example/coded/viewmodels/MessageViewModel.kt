package com.example.coded.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MessageViewModel(
    private val messageRepository: EnhancedMessageRepository
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val messages: StateFlow<List<ConversationMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    suspend fun getOrCreateConversation(userId1: String, userId2: String): String {
        return messageRepository.getOrCreateConversation(userId1, userId2)
    }

    fun loadConversationMessages(conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                messageRepository.observeMessages(conversationId).collect { messages ->
                    _messages.value = messages
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String,
        listingId: String? = null
    ) {
        viewModelScope.launch {
            try {
                messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content,
                    listingId = listingId
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
            }
        }
    }

    fun markMessagesAsRead(conversationId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.markMessagesAsRead(conversationId, userId)
        }
    }

    fun markMessagesAsDelivered(conversationId: String, userId: String) {
        // Note: Your repository doesn't have markMessagesAsDelivered, so we'll use markMessagesAsRead
        viewModelScope.launch {
            messageRepository.markMessagesAsRead(conversationId, userId)
        }
    }

    fun addReaction(conversationId: String, messageId: String, userId: String, emoji: String) {
        viewModelScope.launch {
            messageRepository.addReaction(conversationId, messageId, userId, emoji)
        }
    }

    fun deleteMessage(conversationId: String, messageId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(conversationId, messageId, userId)
        }
    }

    fun getQuickReplies(): List<String> = messageRepository.getQuickReplies()
}