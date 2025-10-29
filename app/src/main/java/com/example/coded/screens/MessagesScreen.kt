package com.example.coded.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coded.data.AuthRepository
import com.example.coded.data.Chat
import com.example.coded.data.Message
import com.example.coded.data.MessageRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    authRepository: AuthRepository,
    listingId: String? = null,
    sellerId: String? = null
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val messageRepository = remember { MessageRepository() }
    val coroutineScope = rememberCoroutineScope()

    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var messageText by remember { mutableStateOf("") }
    var isInChatMode by remember { mutableStateOf(false) }
    var currentChatId by remember { mutableStateOf<String?>(null) }

    // Load chats when screen opens
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            messageRepository.getUserChats(currentUser!!.id).collectLatest { chatList ->
                chats = chatList
                println("✅ Loaded ${chats.size} chats for user")
                isLoading = false
            }
        }
    }

    // Handle direct chat opening from listing
    LaunchedEffect(listingId, sellerId, currentUser) {
        if (listingId != null && sellerId != null && currentUser != null) {
            isInChatMode = true
            currentChatId = generateChatId(currentUser!!.id, sellerId, listingId)

            println("🔄 Opening direct chat: $currentChatId")

            // Load messages for this chat
            messageRepository.getMessages(currentChatId!!).collectLatest { messageList ->
                messages = messageList
                println("✅ Loaded ${messages.size} messages for direct chat")
                isLoading = false
            }

            // Mark messages as read when opening chat
            coroutineScope.launch {
                messageRepository.markMessagesAsRead(currentChatId!!, currentUser!!.id)
            }
        }
    }

    // Handle chat selection with real-time updates
    LaunchedEffect(currentChatId) {
        if (currentChatId != null && currentUser != null) {
            println("🔄 Setting up real-time messages for chat: $currentChatId")

            messageRepository.getMessages(currentChatId!!).collectLatest { messageList ->
                messages = messageList
                println("✅ Real-time update: ${messages.size} messages in chat")
            }

            // Mark messages as read when opening chat
            coroutineScope.launch {
                messageRepository.markMessagesAsRead(currentChatId!!, currentUser!!.id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isInChatMode) "Chat" else "Messages")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isInChatMode) {
                            isInChatMode = false
                            currentChatId = null
                            // Reload chats when going back
                            if (currentUser != null) {
                                coroutineScope.launch {
                                    messageRepository.getUserChats(currentUser!!.id).collectLatest { chatList ->
                                        chats = chatList
                                    }
                                }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF013B33))
                    }
                }

                isInChatMode -> {
                    // Chat View
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Messages List
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No messages yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Start a conversation",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                    // Debug info
                                    Text(
                                        text = "Chat ID: ${currentChatId?.take(20)}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                reverseLayout = true
                            ) {
                                items(messages.reversed()) { message ->
                                    ChatMessageBubble(
                                        message = message,
                                        isCurrentUser = message.senderId == currentUser?.id
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // Message Input
                        Surface(
                            tonalElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Type a message...") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF013B33),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (messageText.isNotBlank() && currentUser != null && currentChatId != null) {
                                            coroutineScope.launch {
                                                try {
                                                    // Extract participant IDs from chat ID
                                                    val parts = currentChatId!!.split("_")
                                                    val otherParticipant = if (parts[0] == currentUser!!.id) parts[1] else parts[0]
                                                    val listingIdFromChat = parts[2]

                                                    val message = Message(
                                                        id = UUID.randomUUID().toString(),
                                                        listingId = listingIdFromChat,
                                                        senderId = currentUser!!.id,
                                                        receiverId = otherParticipant,
                                                        content = messageText,
                                                        isRead = false,
                                                        createdAt = Timestamp.now(),
                                                        chatId = currentChatId!! // ✅ Add chatId here
                                                    )

                                                    println("📤 MessagesScreen: Sending message:")
                                                    println("   Chat ID: ${message.chatId}")
                                                    println("   From: ${message.senderId} → To: ${message.receiverId}")
                                                    println("   Content: ${message.content}")

                                                    val success = messageRepository.sendMessage(message)
                                                    if (success) {
                                                        messageText = ""
                                                        println("✅ MessagesScreen: Message sent successfully!")
                                                    } else {
                                                        println("❌ MessagesScreen: Failed to send message")
                                                    }
                                                } catch (e: Exception) {
                                                    println("❌ MessagesScreen: Error sending message: ${e.message}")
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    },
                                    enabled = messageText.isNotBlank() && currentChatId != null,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color(0xFF013B33),
                                        disabledContainerColor = Color.Gray
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                chats.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No messages yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chat with sellers and buyers here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chats) { chat ->
                            ChatItem(
                                chat = chat,
                                currentUserId = currentUser?.id ?: "",
                                onClick = {
                                    isInChatMode = true
                                    currentChatId = chat.id
                                    println("🔄 Opening chat: ${chat.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// CHANGED: Renamed to avoid conflict with ChatScreen's MessageBubble
@Composable
fun ChatMessageBubble(message: Message, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isCurrentUser) Color(0xFF013B33) else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            shadowElevation = if (isCurrentUser) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(message.createdAt.toDate()),
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatItem(chat: Chat, currentUserId: String, onClick: () -> Unit) {
    val otherParticipant = if (chat.participant1 == currentUserId) chat.participant2 else chat.participant1
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF013B33),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "User ${otherParticipant.takeLast(6)}", // Show last 6 chars for better identification
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF013B33)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dateFormat.format(chat.lastMessageTime.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF013B33),
                        shape = CircleShape
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper function to generate chat ID (same as in repository)
private fun generateChatId(user1: String, user2: String, listingId: String): String {
    val participants = listOf(user1, user2).sorted()
    return "${participants[0]}_${participants[1]}_$listingId"
}