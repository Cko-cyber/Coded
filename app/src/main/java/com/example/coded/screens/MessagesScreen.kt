package com.example.coded.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coded.data.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * 💬 ENHANCED MESSAGES SCREEN - 2025 Edition
 *
 * Features:
 * - Swipe to delete conversations
 * - Search conversations
 * - Unread message badges
 * - Online status indicators
 * - Typing indicators
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedMessagesScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val messageRepository = remember { EnhancedMessageRepository() }

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<Conversation?>(null) }

    // Load conversations with real-time updates
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            messageRepository.listenToConversations(user.id).collectLatest {
                conversations = it
            }
        }
    }

    // Filter conversations based on search
    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conv ->
                val otherUser = conv.participantDetails.values.firstOrNull { it.userId != currentUser?.id }
                otherUser?.name?.contains(searchQuery, ignoreCase = true) == true ||
                        conv.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Delete Conversation?") },
            text = {
                Text("This will permanently delete this conversation. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Implement delete conversation in repository
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Messages") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF013B33),
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { /* TODO: Settings */ }) {
                            Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                        }
                    }
                )

                // Search Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF013B33),
                    shadowElevation = 4.dp
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search conversations...") },
                        leadingIcon = { Icon(Icons.Default.Search, "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
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
                filteredConversations.isEmpty() && searchQuery.isNotEmpty() -> {
                    // No search results
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No results found",
                        subtitle = "Try a different search term"
                    )
                }
                conversations.isEmpty() -> {
                    // No conversations at all
                    EmptyState(
                        icon = Icons.Default.Chat,
                        title = "No conversations yet",
                        subtitle = "Start chatting with sellers!"
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = filteredConversations,
                            key = { it.id }
                        ) { conversation ->
                            SwipeableConversationItem(
                                conversation = conversation,
                                currentUserId = currentUser?.id ?: "",
                                onClick = {
                                    val otherUserId = conversation.participants.first { it != currentUser?.id }
                                    navController.navigate("chat/$otherUserId/null")
                                },
                                onDelete = {
                                    showDeleteDialog = conversation
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🔄 SWIPEABLE CONVERSATION ITEM
 * Swipe left to reveal delete button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipe = -200f
    val haptic = LocalHapticFeedback.current

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "swipe_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        // Delete background (revealed on swipe)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Delete",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Main conversation item
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < maxSwipe / 2) {
                                offsetX = maxSwipe
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                offsetX = 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (offsetX + dragAmount).coerceIn(maxSwipe, 0f)
                            offsetX = newOffset
                        }
                    )
                },
            onClick = {
                if (offsetX < 0) {
                    offsetX = 0f // Close swipe if open
                } else {
                    onClick()
                }
            },
            color = Color.White
        ) {
            ConversationItemContent(
                conversation = conversation,
                currentUserId = currentUserId,
                isSwipeRevealed = animatedOffset < -50f,
                onDeleteClick = {
                    onDelete()
                    offsetX = 0f
                }
            )
        }
    }
}

@Composable
fun ConversationItemContent(
    conversation: Conversation,
    currentUserId: String,
    isSwipeRevealed: Boolean,
    onDeleteClick: () -> Unit
) {
    val otherUser = conversation.participantDetails.values.firstOrNull { it.userId != currentUserId }
    val unreadCount = conversation.unreadCount[currentUserId] ?: 0
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val isToday = remember(conversation.lastMessageTime) {
        val today = Calendar.getInstance()
        val messageDate = Calendar.getInstance().apply {
            time = conversation.lastMessageTime?.toDate() ?: Date()
        }
        today.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        text = (otherUser?.name?.firstOrNull()?.toString() ?: "?").uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Online indicator
            if (otherUser?.isOnline == true) {
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

        // Message content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = otherUser?.name ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Typing indicator or last message
            if (conversation.typingStatus[otherUser?.userId] == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "typing",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                    TypingIndicator()
                }
            } else {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unreadCount > 0) Color.Black else Color.Gray,
                    maxLines = 2,
                    fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Time and unread badge
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = conversation.lastMessageTime?.toDate()?.let { date ->
                    if (isToday) timeFormat.format(date) else dateFormat.format(date)
                } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = if (unreadCount > 0) Color(0xFF013B33) else Color.Gray,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
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

        // Show delete button when swiped
        AnimatedVisibility(
            visible = isSwipeRevealed,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}

/**
 * 💭 TYPING INDICATOR ANIMATION
 */
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotScale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dotScale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dotScale3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.padding(start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size((6 * dotScale1).dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Box(
            modifier = Modifier
                .size((6 * dotScale2).dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Box(
            modifier = Modifier
                .size((6 * dotScale3).dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}