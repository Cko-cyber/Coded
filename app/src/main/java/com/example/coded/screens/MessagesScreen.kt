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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coded.data.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontStyle

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedMessagesScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val messageRepository = remember { EnhancedMessageRepository() }
    val coroutineScope = rememberCoroutineScope()

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var messages by remember { mutableStateOf<List<ConversationMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var selectedListingContext by remember { mutableStateOf<ListingContext?>(null) }
    var showQuickReplies by remember { mutableStateOf(false) }

    // Listen to typing changes and update Firestore
    LaunchedEffect(messageText) {
        snapshotFlow { messageText.isNotEmpty() }
            .debounce(300)
            .collectLatest { typing ->
                if (isTyping != typing && selectedConversation != null) {
                    isTyping = typing
                    messageRepository.updateTypingStatus(
                        selectedConversation!!.id,
                        currentUser!!.id,
                        typing
                    )
                }
            }
    }

    // Load conversations
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            messageRepository.listenToConversations(user.id).collectLatest {
                conversations = it
            }
        }
    }

    // Load messages when conversation is selected
    LaunchedEffect(selectedConversation) {
        selectedConversation?.let { conv ->
            messageRepository.listenToMessages(conv.id).collectLatest {
                messages = it
                // Mark as read and delivered
                currentUser?.let { user ->
                    messageRepository.markMessagesAsRead(conv.id, user.id)
                    messageRepository.markMessagesAsDelivered(conv.id, user.id)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedConversation != null) {
                        val otherUser = selectedConversation!!.participantDetails.values
                            .first { it.userId != currentUser?.id }

                        Column {
                            Text(otherUser.name)
                            if (selectedConversation!!.typingStatus[otherUser.userId] == true) {
                                Text(
                                    "typing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            } else {
                                Text(
                                    if (otherUser.isOnline) "Online" else "Last seen recently",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Text("Messages")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedConversation != null) {
                            selectedConversation = null
                            messages = emptyList()
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
                ),
                actions = {
                    if (selectedConversation != null) {
                        IconButton(onClick = { /* Video call */ }) {
                            Icon(Icons.Default.Videocam, "Video Call", tint = Color.White)
                        }
                        IconButton(onClick = { /* Voice call */ }) {
                            Icon(Icons.Default.Call, "Voice Call", tint = Color.White)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedConversation != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Listing context selector
                        if (selectedConversation!!.listingContexts.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedConversation!!.listingContexts.values.toList()) { context ->
                                    FilterChip(
                                        selected = selectedListingContext?.listingId == context.listingId,
                                        onClick = { selectedListingContext = context },
                                        label = {
                                            Text(
                                                "${context.listingTitle} (${context.messageCount})",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Quick replies
                        if (showQuickReplies) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messageRepository.getQuickReplies()) { reply ->
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
                            // Quick reply toggle
                            IconButton(onClick = { showQuickReplies = !showQuickReplies }) {
                                Icon(Icons.Default.Quickreply, "Quick Replies")
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
                                    if (messageText.isNotBlank() && currentUser != null) {
                                        val otherUserId = selectedConversation!!.participants
                                            .first { it != currentUser!!.id }

                                        coroutineScope.launch {
                                            messageRepository.sendMessage(
                                                conversationId = selectedConversation!!.id,
                                                senderId = currentUser!!.id,
                                                receiverId = otherUserId,
                                                content = messageText.trim(),
                                                listingId = selectedListingContext?.listingId
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
                                Icon(Icons.Default.Send, "Send", tint = Color.White)
                            }
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
            if (selectedConversation == null) {
                // Conversations list
                ConversationsList(
                    conversations = conversations,
                    currentUserId = currentUser?.id ?: "",
                    onConversationClick = { conv ->
                        selectedConversation = conv
                        selectedListingContext = conv.listingContexts.values.firstOrNull()
                    }
                )
            } else {
                // Chat view
                ChatView(
                    messages = messages,
                    currentUserId = currentUser?.id ?: "",
                    onReaction = { messageId, emoji ->
                        coroutineScope.launch {
                            messageRepository.addReaction(
                                selectedConversation!!.id,
                                messageId,
                                currentUser!!.id,
                                emoji
                            )
                        }
                    },
                    onDelete = { messageId ->
                        coroutineScope.launch {
                            messageRepository.deleteMessage(
                                selectedConversation!!.id,
                                messageId,
                                currentUser!!.id
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ConversationsList(
    conversations: List<Conversation>,
    currentUserId: String,
    onConversationClick: (Conversation) -> Unit
) {
    if (conversations.isEmpty()) {
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
                Text("No conversations yet", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversations) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    currentUserId = currentUserId,
                    onClick = { onConversationClick(conversation) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    val otherUser = conversation.participantDetails.values.first { it.userId != currentUserId }
    val unreadCount = conversation.unreadCount[currentUserId] ?: 0
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (unreadCount > 0) Color(0xFF013B33).copy(alpha = 0.05f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture with online indicator
            Box {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF013B33),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = otherUser.name.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (otherUser.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherUser.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Show typing indicator
                if (conversation.typingStatus[otherUser.userId] == true) {
                    Text(
                        text = "typing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                // Show listing context if available
                if (conversation.lastMessageListingId != null) {
                    val context = conversation.listingContexts[conversation.lastMessageListingId]
                    context?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📍 ${it.listingTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF013B33)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = conversation.lastMessageTime?.toDate()?.let { date ->
                        dateFormat.format(date)
                    } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF013B33),
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatView(
    messages: List<ConversationMessage>,
    currentUserId: String,
    onReaction: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val listState = rememberLazyListState()
    var selectedMessage by remember { mutableStateOf<ConversationMessage?>(null) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            val isCurrentUser = message.senderId == currentUserId

            // Skip if deleted for this user
            if (message.deletedFor.contains(currentUserId)) return@items

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onClick = { },
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
                        // Listing context
                        message.listingSnapshot?.let { listing ->
                            Surface(
                                color = if (isCurrentUser) Color.White.copy(alpha = 0.2f)
                                else Color(0xFF013B33).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        listing.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentUser) Color.White else Color.Black
                                    )
                                    Text(
                                        "E ${listing.price}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCurrentUser) Color.White.copy(0.8f) else Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

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
                                text = message.createdAt?.toDate()?.let { date ->
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                                } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrentUser) Color.White.copy(0.7f) else Color.Gray
                            )

                            if (isCurrentUser) {
                                Icon(
                                    imageVector = when (message.status) {
                                        "READ" -> Icons.Default.DoneAll
                                        "DELIVERED" -> Icons.Default.DoneAll
                                        else -> Icons.Default.Done
                                    },
                                    contentDescription = message.status,
                                    tint = if (message.status == "READ") Color(0xFF4CAF50) else Color.White.copy(0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
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
    }

    // Message action dialog
    if (selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = { Text("Message Actions") },
            text = {
                Column {
                    val reactions = listOf("👍", "❤️", "😊", "🔥", "👏")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reactions) { emoji ->
                            TextButton(onClick = {
                                onReaction(selectedMessage!!.id, emoji)
                                selectedMessage = null
                            }) {
                                Text(emoji, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(selectedMessage!!.id)
                    selectedMessage = null
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