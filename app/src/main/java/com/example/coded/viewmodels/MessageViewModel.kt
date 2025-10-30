package com.example.coded.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Chat
import com.example.coded.data.Message
import com.example.coded.data.MessageRepository
import com.example.coded.data.MessageStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MessageViewModel(
    private val repository: MessageRepository = MessageRepository()
) : ViewModel() {

    // Real-time list of chats
    private val _userChats = MutableStateFlow<List<Chat>>(emptyList())
    val userChats: StateFlow<List<Chat>> = _userChats.asStateFlow()

    // Real-time list of messages for current chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // For UI loading and status states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ Load chats for a specific user in real-time
    fun loadUserChats(userId: String) {
        viewModelScope.launch {
            repository.getUserChats(userId)
                .catch { e -> _errorMessage.value = e.message }
                .collect { chats -> _userChats.value = chats }
        }
    }

    // ✅ Load messages for a specific chat
    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            repository.getMessages(chatId)
                .catch { e -> _errorMessage.value = e.message }
                .collect { msgs -> _messages.value = msgs }
        }
    }

    // ✅ Send message
    fun sendMessage(message: Message) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.sendMessage(message)
            _isLoading.value = false
            if (!success) {
                _errorMessage.value = "Failed to send message"
            }
        }
    }

    // ✅ Mark messages as read (when chat is opened)
    fun markMessagesAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            repository.markMessagesAsRead(chatId, userId)
        }
    }

    // ✅ Update message status (delivered, read, etc.)
    fun updateMessageStatus(messageId: String, status: MessageStatus) {
        viewModelScope.launch {
            repository.updateMessageStatus(messageId, status)
        }
    }
}
