package com.example.coded.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Quickreply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coded.AppModule
import com.example.coded.data.*
import com.example.coded.viewmodels.MessageViewModel
import com.example.coded.viewmodels.MessageViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedChatScreen(
    navController: NavController,
    otherUserId: String,
    listingId: String?,
    authRepository: AuthRepository
) {
    // --- ViewModel (use factory so Compose can construct it properly) ---
    val viewModel: MessageViewModel = viewModel(
        factory = MessageViewModelFactory(AppModule.messageRepository)
    )

    // --- State from Auth + ViewModel ---
    val currentUser by authRepository.currentUser.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var conversationId by remember { mutableStateOf<String?>(null) }
    var showQuickReplies by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ConversationMessage?>(null) }

    // Create/get conversation when we have currentUser
    LaunchedEffect(currentUser?.id, otherUserId) {
        val meId = currentUser?.id
        if (!meId.isNullOrBlank()) {
            try {
                val convId = viewModel.getOrCreateConversation(meId, otherUserId)
                conversationId = convId
                viewModel.loadConversationMessages(convId)
            } catch (e: Exception) {
                // propagate to viewModel error state
            }
        }
    }

    // Mark as read when opening
    LaunchedEffect(conversationId, currentUser?.id) {
        val conv = conversationId
        val meId = currentUser?.id
        if (!conv.isNullOrBlank() && !meId.isNullOrBlank()) {
            viewModel.markMessagesAsRead(conv, meId)
            viewModel.markMessagesAsDelivered(conv, meId)
        }
    }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    if (listingId != null) {
                        IconButton(onClick = {
                            navController.navigate("single_stock/$listingId")
                        }) {
                            Icon(Icons.Filled.Info, contentDescription = "View Listing", tint = Color.White)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick replies
                    if (showQuickReplies) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.getQuickReplies()) { reply ->
                                SuggestionChip(
                                    onClick = {
                                        messageText = reply
                                        showQuickReplies = false
                                    },
                                    label = { Text(reply, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    // Message input row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick reply toggle
                        IconButton(onClick = { showQuickReplies = !showQuickReplies }) {
                            Icon(
                                if (showQuickReplies) Icons.Filled.Close else Icons.Rounded.Quickreply,
                                contentDescription = "Quick Replies"
                            )
                        }

                        // Text input
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send button
                        IconButton(
                            onClick = {
                                val meId = currentUser?.id
                                val conv = conversationId
                                if (messageText.isNotBlank() && !meId.isNullOrBlank() && !conv.isNullOrBlank()) {
                                    coroutineScope.launch {
                                        viewModel.sendMessage(
                                            conversationId = conv,
                                            senderId = meId,
                                            receiverId = otherUserId,
                                            content = messageText.trim(),
                                            listingId = listingId
                                        )
                                        messageText = ""
                                        showQuickReplies = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF013B33), CircleShape),
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
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
            when {
                isLoading && messages.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF013B33)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "", color = Color.Red)
                    }
                }
                messages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No messages yet", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Start the conversation!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { message ->
                            val isCurrentUser = message.senderId == currentUser?.id

                            // If message is soft-deleted for this user, skip
                            if (message.deletedFor.contains(currentUser?.id)) return@items

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .combinedClickable(
                                            onClick = { /* no-op */ },
                                            onLongClick = { selectedMessage = message }
                                        ),
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
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = message.content,
                                            color = if (isCurrentUser) Color.White else Color.Black
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Created time
                                            Text(
                                                text = try {
                                                    SimpleDateFormat("HH:mm", Locale.getDefault())
                                                        .format(message.createdAt.toDate())
                                                } catch (e: Exception) {
                                                    ""
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCurrentUser) Color.White.copy(0.7f) else Color.Gray
                                            )

                                            // Delivery/read icons for own messages
                                            if (isCurrentUser) {
                                                Icon(
                                                    imageVector = when (message.status) {
                                                        "READ" -> Icons.Filled.ArrowBack /* placeholder, keep icons as needed */
                                                        "DELIVERED" -> Icons.Filled.ArrowBack
                                                        else -> Icons.Filled.ArrowBack
                                                    },
                                                    contentDescription = message.status,
                                                    tint = if (message.status == "READ") Color(0xFF4CAF50) else Color.White.copy(0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Reactions (if any)
                                        if (!message.reactions.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = message.reactions.values.joinToString(" "),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Message actions dialog (react / delete)
    if (selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = { Text("Message Actions") },
            text = {
                Column {
                    Text("React to this message:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val reactions = listOf("👍", "❤️", "😊", "🔥", "👏")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reactions) { emoji ->
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    val conv = conversationId
                                    val meId = currentUser?.id
                                    if (!conv.isNullOrBlank() && !meId.isNullOrBlank() && selectedMessage != null) {
                                        viewModel.addReaction(conv, selectedMessage!!.id, meId, emoji)
                                    }
                                    selectedMessage = null
                                }
                            }) {
                                Text(emoji, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val conv = conversationId
                        val meId = currentUser?.id
                        if (!conv.isNullOrBlank() && !meId.isNullOrBlank() && selectedMessage != null) {
                            viewModel.deleteMessage(conv, selectedMessage!!.id, meId)
                        }
                        selectedMessage = null
                    }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
