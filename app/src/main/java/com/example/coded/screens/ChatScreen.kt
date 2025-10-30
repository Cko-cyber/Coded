package com.example.coded.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.coded.data.Message
import com.example.coded.data.MessageRepository
import com.example.coded.data.MessageStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    listingId: String,
    sellerId: String,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val firestore = FirebaseFirestore.getInstance()
    val messageRepository = remember { MessageRepository() }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var sellerName by remember { mutableStateOf("Seller") }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Generate chat ID
    val chatId = remember(currentUser?.id, sellerId, listingId) {
        generateChatId(currentUser?.id ?: "", sellerId, listingId)
    }

    // Load seller's real name
    LaunchedEffect(sellerId) {
        val user = messageRepository.getUserById(sellerId)
        sellerName = user?.full_name ?: "User ${sellerId.takeLast(6)}"
    }

    // Real-time message listener
    LaunchedEffect(chatId, currentUser) {
        if (currentUser != null) {
            println("🔄 ChatScreen: Setting up listener for chat: $chatId")

            listenerRegistration?.remove()

            listenerRegistration = firestore.collection("messages")
                .whereEqualTo("chat_id", chatId)
                .orderBy("created_at", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        println("❌ ChatScreen listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val newMessages = snapshot.documents.mapNotNull { doc ->
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

                        messages = newMessages
                        println("✅ ChatScreen: Loaded ${messages.size} messages for chat: $chatId")
                        isLoading = false

                        // Auto-scroll to bottom
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }

                        // Update message statuses
                        coroutineScope.launch {
                            // Mark messages as delivered
                            messages.forEach { message ->
                                if (message.receiverId == currentUser!!.id &&
                                    message.status == MessageStatus.SENT) {
                                    messageRepository.updateMessageToDelivered(message.id)
                                }
                            }

                            // Mark messages as read
                            messageRepository.markMessagesAsRead(chatId, currentUser!!.id)
                        }
                    }
                }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(sellerName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Online", // Simple online status
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        navController.navigate("single_stock/$listingId")
                    }) {
                        Icon(Icons.Default.Info, "Listing Info", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            MessageInputSection(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank() && currentUser != null && !isSending) {
                        isSending = true
                        coroutineScope.launch {
                            val message = Message(
                                id = UUID.randomUUID().toString(),
                                listingId = listingId,
                                senderId = currentUser!!.id,
                                receiverId = sellerId,
                                content = messageText.trim(),
                                isRead = false,
                                status = MessageStatus.SENT,
                                createdAt = Timestamp.now(),
                                chatId = chatId
                            )

                            println("📤 ChatScreen: Sending message to chat: $chatId")

                            val success = messageRepository.sendMessage(message)
                            if (success) {
                                messageText = ""
                                println("✅ ChatScreen: Message sent and input cleared!")
                            } else {
                                println("❌ ChatScreen: Failed to send message")
                            }
                            isSending = false
                        }
                    }
                },
                isSending = isSending
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF013B33)
                )
            } else if (messages.isEmpty()) {
                EmptyChatState(sellerName = sellerName)
            } else {
                MessagesList(
                    messages = messages,
                    currentUserId = currentUser?.id ?: "",
                    listState = listState
                )
            }
        }
    }
}

@Composable
fun MessageInputSection(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isSending: Boolean
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 3,
                enabled = !isSending,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF013B33),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendMessage,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF013B33), RoundedCornerShape(24.dp)),
                enabled = messageText.isNotBlank() && !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(sellerName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages with $sellerName yet",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start the conversation!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun MessagesList(
    messages: List<Message>,
    currentUserId: String,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            EnhancedMessageBubble(
                message = message,
                isCurrentUser = message.senderId == currentUserId
            )
        }
    }
}

@Composable
fun EnhancedMessageBubble(message: Message, isCurrentUser: Boolean) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser) Color(0xFF013B33) else Color.White
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            )
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

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // WhatsApp-like status indicators
                    if (isCurrentUser) {
                        val (icon, color) = when (message.status) {
                            MessageStatus.SENT -> Pair(Icons.Default.Done, Color.White.copy(alpha = 0.7f))
                            MessageStatus.DELIVERED -> Pair(Icons.Default.DoneAll, Color.White.copy(alpha = 0.7f))
                            MessageStatus.READ -> Pair(Icons.Default.DoneAll, Color(0xFF4CAF50))
                        }

                        Icon(
                            icon,
                            contentDescription = "Message status",
                            modifier = Modifier.size(12.dp),
                            tint = color
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = dateFormat.format(message.createdAt.toDate()),
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun generateChatId(user1: String, user2: String, listingId: String): String {
    val participants = listOf(user1, user2).sorted()
    return "${participants[0]}_${participants[1]}_$listingId"
}