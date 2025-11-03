package com.example.coded.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.coded.data.Listing
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 📋 Booking Details Screen
 *
 * Shows detailed booking request information when seller taps notification
 * Supports both Call Bookings and Viewing Bookings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    navController: NavController,
    authRepository: AuthRepository,
    listingId: String,
    bookingType: String, // "call" or "viewing"
    buyerName: String?,
    buyerId: String?,
    preferredDate: String?,
    preferredTime: String?,
    numberOfPeople: String? = null,
    additionalMessage: String? = null
) {
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState()

    var listing by remember { mutableStateOf<Listing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }
    var declineReason by remember { mutableStateOf("") }

    // Load listing details
    LaunchedEffect(listingId) {
        try {
            val doc = firestore.collection("listings").document(listingId).get().await()
            listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    // Confirm Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Confirm ${if (bookingType == "call") "Call" else "Viewing"}") },
            text = { Text("Confirm this ${if (bookingType == "call") "call" else "viewing"} request from $buyerName?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // TODO: Update booking status in Firestore
                            // TODO: Send confirmation notification to buyer
                            showConfirmDialog = false
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Decline Dialog with reason
    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Decline Request") },
            text = {
                Column {
                    Text("Provide a reason for declining (optional):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = declineReason,
                        onValueChange = { declineReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Not available at that time") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // TODO: Update booking status to declined
                            // TODO: Send decline notification with reason
                            showDeclineDialog = false
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Decline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Request") },
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
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF013B33))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Booking Type Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (bookingType == "call") Color(0xFF2196F3) else Color(0xFFFF6F00)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (bookingType == "call") Icons.Default.Call else Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (bookingType == "call") "Phone Call Request" else "Viewing Request",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "New booking request",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Listing Information
                listing?.let { listingData ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Listing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF013B33)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = listingData.breed,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "E ${listingData.price}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buyer Information
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Buyer Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow(icon = Icons.Default.Person, label = "Name", value = buyerName ?: "Unknown")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Booking Details
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Booking Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Date",
                            value = preferredDate ?: "Not specified"
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            icon = Icons.Default.AccessTime,
                            label = "Time",
                            value = preferredTime ?: "Not specified"
                        )

                        if (bookingType == "viewing" && numberOfPeople != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(
                                icon = Icons.Default.Group,
                                label = "Number of People",
                                value = numberOfPeople
                            )
                        }

                        if (!additionalMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Additional Message",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = additionalMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decline Button
                    OutlinedButton(
                        onClick = { showDeclineDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Decline")
                    }

                    // Confirm Button
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Buyer Button
                OutlinedButton(
                    onClick = {
                        buyerId?.let { id ->
                            navController.navigate("chat/$id/$listingId")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF013B33))
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message Buyer")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF013B33),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}