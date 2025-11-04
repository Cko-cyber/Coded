package com.example.coded.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.coded.data.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val listingId: String? = null,
    val senderId: String? = null, // ✅ ADDED: For message notifications
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }
    var showDeleteDialog by remember { mutableStateOf<AppNotification?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val unreadCount = notifications.count { !it.isRead }

    // Real-time notification listener
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            listenerRegistration = firestore.collection("notifications")
                .whereEqualTo("userId", currentUser?.id ?: "")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        notifications = snapshot.documents.mapNotNull { doc ->
                            try {
                                AppNotification(
                                    id = doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    type = doc.getString("type") ?: "",
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    listingId = doc.getString("listingId"),
                                    senderId = doc.getString("senderId"), // ✅ ADDED
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        isLoading = false
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    // ✅ Delete single notification
    fun deleteNotification(notification: AppNotification) {
        coroutineScope.launch {
            try {
                firestore.collection("notifications")
                    .document(notification.id)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    // ✅ Clear all notifications
    fun clearAllNotifications() {
        coroutineScope.launch {
            try {
                val batch = firestore.batch()
                notifications.forEach { notification ->
                    val docRef = firestore.collection("notifications").document(notification.id)
                    batch.delete(docRef)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications")
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFF6F00),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                    // ✅ Mark all as read
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val batch = firestore.batch()
                                    notifications.filter { !it.isRead }.forEach { notification ->
                                        val docRef = firestore.collection("notifications").document(notification.id)
                                        batch.update(docRef, "isRead", true)
                                    }
                                    batch.commit().await()
                                }
                            }
                        ) {
                            Text("Mark all read", color = Color.White)
                        }
                    }

                    // ✅ More options menu
                    if (notifications.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }

                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear all notifications") },
                                    onClick = {
                                        expanded = false
                                        showClearAllDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteSweep, null, tint = Color.Red)
                                    }
                                )
                            }
                        }
                    }
                }
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF013B33)
                    )
                }
                notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notifications yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We'll notify you when there's activity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    // Mark as read
                                    coroutineScope.launch {
                                        if (!notification.isRead) {
                                            firestore.collection("notifications")
                                                .document(notification.id)
                                                .update("isRead", true)
                                                .await()
                                        }
                                    }

                                    // ✅ UPDATED: Navigate based on notification type
                                    when (notification.type) {
                                        "new_message", "message" -> {
                                            // Navigate to chat screen with the sender
                                            val senderId = notification.senderId ?: notification.listingId ?: ""
                                            if (senderId.isNotBlank() && currentUser?.id != null) {
                                                navController.navigate("chat/$senderId/null")
                                            } else {
                                                // Fallback to messages screen if no senderId
                                                navController.navigate("messages")
                                            }
                                        }
                                        "call_booking", "viewing_booking" -> {
                                            // Navigate to booking details screen
                                            notification.listingId?.let { listingId ->
                                                val bookingType = when (notification.type) {
                                                    "call_booking" -> "call"
                                                    "viewing_booking" -> "viewing"
                                                    else -> "call"
                                                }
                                                navController.navigate("booking_details/$listingId/$bookingType")
                                            } ?: run {
                                                // Fallback to listing if no booking details
                                                notification.listingId?.let { listingId ->
                                                    navController.navigate("single_stock/$listingId")
                                                }
                                            }
                                        }
                                        "listing_interest" -> {
                                            // Navigate to listing details
                                            notification.listingId?.let { listingId ->
                                                navController.navigate("single_stock/$listingId")
                                            }
                                        }
                                        else -> {
                                            // Default navigation
                                            notification.listingId?.let { listingId ->
                                                navController.navigate("single_stock/$listingId")
                                            }
                                        }
                                    }
                                },
                                onLongClick = {
                                    showDeleteDialog = notification
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Delete single notification dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Notification") },
            text = { Text("Delete this notification?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteNotification(showDeleteDialog!!)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ✅ Clear all notifications dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(Icons.Default.DeleteSweep, null, tint = Color.Red, modifier = Modifier.size(48.dp))
            },
            title = { Text("Clear All Notifications") },
            text = { Text("Are you sure you want to delete all ${notifications.size} notifications? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        clearAllNotifications()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val (icon, iconColor) = when (notification.type) {
        "message", "new_message" -> Icons.Default.Chat to Color(0xFF013B33)
        "call_booking" -> Icons.Default.Call to Color(0xFF2196F3)
        "viewing_booking" -> Icons.Default.CalendarToday to Color(0xFFFF6F00)
        else -> Icons.Default.Notifications to Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFF013B33).copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dateFormat.format(notification.createdAt.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6F00))
                )
            }
        }
    }
}