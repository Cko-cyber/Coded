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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    authRepository: AuthRepository,
    listingId: String,
    sellerId: String
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val chatId = remember {
        generateChatId(currentUser?.id ?: "", sellerId, listingId)
    }

    // Load messages
    LaunchedEffect(chatId) {
        try {
            firestore.collection("messages")
                .whereEqualTo("chatId", chatId)
                .orderBy("created_at", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    messages = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    } ?: emptyList()

                    isLoading = false

                    // Scroll to bottom when new message arrives
                    coroutineScope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with Seller") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Message Input
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF013B33),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank() && !isSending && currentUser != null) {
                                coroutineScope.launch {
                                    isSending = true

                                    val message = Message(
                                        id = UUID.randomUUID().toString(),
                                        listingId = listingId,
                                        senderId = currentUser!!.id,
                                        receiverId = sellerId,
                                        content = messageText.trim(),
                                        isRead = false,
                                        createdAt = Timestamp.now()
                                    )

                                    try {
                                        // Save message
                                        firestore.collection("messages")
                                            .document(message.id)
                                            .set(message.toMap())
                                            .await()

                                        // Update or create chat
                                        val chat = mapOf(
                                            "id" to chatId,
                                            "participant1" to currentUser!!.id,
                                            "participant2" to sellerId,
                                            "listingId" to listingId,
                                            "lastMessage" to message.content,
                                            "lastMessageTime" to Timestamp.now(),
                                            "participants" to listOf(currentUser!!.id, sellerId)
                                        )

                                        firestore.collection("chats")
                                            .document(chatId)
                                            .set(chat)
                                            .await()

                                        messageText = ""
                                    } catch (e: Exception) {
                                        // Handle error
                                    }

                                    isSending = false
                                }
                            }
                        },
                        containerColor = Color(0xFF013B33),
                        modifier = Modifier.size(56.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
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
                        text = "Start the conversation!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == currentUser?.id
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isCurrentUser: Boolean) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser) Color(0xFF013B33) else Color.White
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser) Color.White else Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = timeFormat.format(message.createdAt.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            modifier = Modifier.size(16.dp),
                            tint = if (message.isRead) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

fun generateChatId(user1: String, user2: String, listingId: String): String {
    val participants = listOf(user1, user2).sorted()
    return "${participants[0]}_${participants[1]}_$listingId"
}

// Extension function to convert Message to Map
fun Message.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "chatId" to generateChatId(senderId, receiverId, listingId),
        "listingId" to listingId,
        "senderId" to senderId,
        "receiverId" to receiverId,
        "content" to content,
        "isRead" to isRead,
        "created_at" to createdAt
    )
}