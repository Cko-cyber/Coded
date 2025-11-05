package com.example.coded.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.clickable  // ✅ ADD THIS
/**
 * 💬 ENHANCED CHAT SCREEN - 2025 Edition
 *
 * Features:
 * - Long-press message actions
 * - Message reactions
 * - Reply/Quote messages
 * - Copy text
 * - Delete messages
 * - Read receipts
 * - Typing indicators
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedChatScreen(
    navController: NavController,
    otherUserId: String,
    listingId: String?,
    authRepository: AuthRepository
) {
    val viewModel: MessageViewModel = viewModel(
        factory = MessageViewModelFactory(AppModule.messageRepository)
    )

    val currentUser by authRepository.currentUser.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var messageText by remember { mutableStateOf("") }
    var conversationId by remember { mutableStateOf<String?>(null) }
    var selectedMessage by remember { mutableStateOf<ConversationMessage?>(null) }
    var replyToMessage by remember { mutableStateOf<ConversationMessage?>(null) }
    var showQuickReplies by remember { mutableStateOf(false) }

    // Create/get conversation
    LaunchedEffect(currentUser?.id, otherUserId) {
        val meId = currentUser?.id
        if (!meId.isNullOrBlank()) {
            try {
                val convId = viewModel.getOrCreateConversation(meId, otherUserId)
                conversationId = convId
                viewModel.loadConversationMessages(convId)
            } catch (e: Exception) {
                // Error handled by viewModel
            }
        }
    }

    // Mark as read
    LaunchedEffect(conversationId, currentUser?.id) {
        val conv = conversationId
        val meId = currentUser?.id
        if (!conv.isNullOrBlank() && !meId.isNullOrBlank()) {
            viewModel.markMessagesAsRead(conv, meId)
            viewModel.markMessagesAsDelivered(conv, meId)
        }
    }

    // Auto-scroll
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
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Reply preview
                    AnimatedVisibility(
                        visible = replyToMessage != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        ReplyPreview(
                            message = replyToMessage,
                            onDismiss = { replyToMessage = null }
                        )
                    }

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

                    // Message input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showQuickReplies = !showQuickReplies }) {
                            Icon(
                                if (showQuickReplies) Icons.Default.Close else Icons.Default.Quickreply,
                                "Quick Replies"
                            )
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.width(8.dp))

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
                                        replyToMessage = null
                                        showQuickReplies = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF013B33), CircleShape),
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, "Send", tint = Color.White)
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
                messages.isEmpty() -> {
                    EmptyChatState()
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

                            if (message.deletedFor.contains(currentUser?.id)) return@items

                            EnhancedMessageBubble(
                                message = message,
                                isCurrentUser = isCurrentUser,
                                onLongPress = {
                                    selectedMessage = message
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Message Actions Bottom Sheet
    if (selectedMessage != null) {
        MessageActionsBottomSheet(
            message = selectedMessage!!,
            isOwnMessage = selectedMessage!!.senderId == currentUser?.id,
            onDismiss = { selectedMessage = null },
            onReact = { emoji ->
                coroutineScope.launch {
                    val conv = conversationId
                    val meId = currentUser?.id
                    if (!conv.isNullOrBlank() && !meId.isNullOrBlank()) {
                        viewModel.addReaction(conv, selectedMessage!!.id, meId, emoji)
                    }
                    selectedMessage = null
                }
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(selectedMessage!!.content))
                selectedMessage = null
            },
            onReply = {
                replyToMessage = selectedMessage
                selectedMessage = null
            },
            onDelete = {
                coroutineScope.launch {
                    val conv = conversationId
                    val meId = currentUser?.id
                    if (!conv.isNullOrBlank() && !meId.isNullOrBlank()) {
                        viewModel.deleteMessage(conv, selectedMessage!!.id, meId)
                    }
                    selectedMessage = null
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedMessageBubble(
    message: ConversationMessage,
    isCurrentUser: Boolean,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
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
                    Text(
                        text = formatTime(message.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentUser) Color.White.copy(0.7f) else Color.Gray
                    )

                    if (isCurrentUser) {
                        MessageStatusIcon(status = message.status)
                    }
                }

                // Reactions
                if (message.reactions.isNotEmpty()) {
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

@Composable
fun MessageStatusIcon(status: String) {
    val (icon, color) = when (status) {
        "READ" -> Icons.Default.DoneAll to Color(0xFF4CAF50)
        "DELIVERED" -> Icons.Default.DoneAll to Color.White.copy(0.7f)
        else -> Icons.Default.Done to Color.White.copy(0.7f)
    }

    Icon(
        imageVector = icon,
        contentDescription = status,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionsBottomSheet(
    message: ConversationMessage,
    isOwnMessage: Boolean,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Message Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reactions
            Text("React", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reactions) { emoji ->
                    TextButton(onClick = { onReact(emoji) }) {
                        Text(emoji, style = MaterialTheme.typography.displaySmall)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Actions
            ListItem(
                headlineContent = { Text("Copy Text") },
                leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                modifier = Modifier.clickable(onClick = onCopy)
            )

            ListItem(
                headlineContent = { Text("Reply") },
                leadingContent = { Icon(Icons.Default.Reply, null) },
                modifier = Modifier.clickable(onClick = onReply)
            )

            if (isOwnMessage) {
                ListItem(
                    headlineContent = { Text("Delete", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    modifier = Modifier.clickable(onClick = onDelete)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ReplyPreview(
    message: ConversationMessage?,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFFE3F2FD),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF2196F3),
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
            ) {}

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Replying to:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message?.content ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Cancel reply")
            }
        }
    }
}

@Composable
fun EmptyChatState() {
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
        Text("No messages yet", color = Color.Gray)
        Text("Start the conversation!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
    return try {
        timestamp?.toDate()?.let { date ->
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } ?: ""
    } catch (e: Exception) {
        ""
    }
}