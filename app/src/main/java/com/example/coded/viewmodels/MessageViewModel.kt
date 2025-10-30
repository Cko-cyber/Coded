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

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    // ✅ FIXED: Load conversations with proper error handling
    fun loadUserConversations(userId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                messageRepository.listenToConversations(userId)
                    .catch { e ->
                        _errorMessage.value = "Failed to load conversations: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { conversationsList ->
                        _conversations.value = conversationsList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error setting up conversation listener: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ✅ FIXED: Get or create conversation with loading state
    suspend fun getOrCreateConversation(userId1: String, userId2: String): String {
        _isLoading.value = true
        _errorMessage.value = null

        return try {
            val conversationId = messageRepository.getOrCreateConversation(userId1, userId2)
            _currentConversationId.value = conversationId
            _isLoading.value = false
            conversationId
        } catch (e: Exception) {
            _errorMessage.value = "Failed to create conversation: ${e.message}"
            _isLoading.value = false
            throw e
        }
    }

    // ✅ FIXED: Load messages with proper error handling
    fun loadConversationMessages(conversationId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _currentConversationId.value = conversationId

        viewModelScope.launch {
            try {
                messageRepository.observeMessages(conversationId)
                    .catch { e ->
                        _errorMessage.value = "Failed to load messages: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { messagesList ->
                        _messages.value = messagesList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error setting up message listener: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ✅ FIXED: Send message with error handling
    fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String,
        listingId: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content,
                    listingId = listingId
                )
                if (!success) {
                    _errorMessage.value = "Failed to send message"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ ADDED: Mark messages as read
    fun markMessagesAsRead(conversationId: String, userId: String) {
        viewModelScope.launch {
            try {
                messageRepository.markMessagesAsRead(conversationId, userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to mark messages as read: ${e.message}"
            }
        }
    }

    // ✅ ADDED: Mark messages as delivered
    fun markMessagesAsDelivered(conversationId: String, userId: String) {
        viewModelScope.launch {
            try {
                messageRepository.markMessagesAsDelivered(conversationId, userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to mark messages as delivered: ${e.message}"
            }
        }
    }

    // ✅ ADDED: Add reaction to message
    fun addReaction(conversationId: String, messageId: String, userId: String, emoji: String) {
        viewModelScope.launch {
            try {
                messageRepository.addReaction(conversationId, messageId, userId, emoji)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add reaction: ${e.message}"
            }
        }
    }

    // ✅ ADDED: Delete message (soft delete)
    fun deleteMessage(conversationId: String, messageId: String, userId: String) {
        viewModelScope.launch {
            try {
                messageRepository.deleteMessage(conversationId, messageId, userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete message: ${e.message}"
            }
        }
    }

    // ✅ ADDED: Update typing status
    fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        viewModelScope.launch {
            try {
                messageRepository.updateTypingStatus(conversationId, userId, isTyping)
            } catch (e: Exception) {
                // Don't show error for typing status updates
            }
        }
    }

    // Cache management
    fun clearCache() {
        messageRepository.clearAllCache()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearCurrentConversation() {
        _currentConversationId.value = null
        _messages.value = emptyList()
    }

    fun getQuickReplies(): List<String> = messageRepository.getQuickReplies()
}