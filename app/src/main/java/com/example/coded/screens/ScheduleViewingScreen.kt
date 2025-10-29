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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ViewingBooking(
    val id: String = "",
    val listingId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val preferredDate: String = "",
    val preferredTime: String = "",
    val numberOfPeople: Int = 1,
    val message: String = "",
    val status: String = "pending",
    val createdAt: Timestamp = Timestamp.now()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleViewingScreen(
    navController: NavController,
    listingId: String,
    sellerId: String,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("Morning (9AM - 12PM)") }
    var numberOfPeople by remember { mutableStateOf(1) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val timeSlots = listOf(
        "Morning (9AM - 12PM)",
        "Afternoon (12PM - 3PM)",
        "Late Afternoon (3PM - 6PM)"
    )

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Viewing Scheduled!") },
            text = {
                Text("Your viewing request has been sent to the seller. They will confirm the appointment soon.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Viewing") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6F00))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Schedule Physical Viewing",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Visit and inspect the livestock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Date Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Preferred Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a date within the next 7 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(7) { index ->
                            val calendar = Calendar.getInstance()
                            calendar.add(Calendar.DAY_OF_YEAR, index)
                            val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
                            val dateString = dateFormat.format(calendar.time)

                            OutlinedButton(
                                onClick = { selectedDate = dateString },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedDate == dateString)
                                        Color(0xFFFF6F00).copy(alpha = 0.1f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dateString)
                                    if (selectedDate == dateString) {
                                        Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFFFF6F00))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Preferred Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    timeSlots.forEach { timeSlot ->
                        OutlinedButton(
                            onClick = { selectedTime = timeSlot },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedTime == timeSlot)
                                    Color(0xFFFF6F00).copy(alpha = 0.1f) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(timeSlot)
                                if (selectedTime == timeSlot) {
                                    Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFFFF6F00))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Number of People
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Number of People",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (numberOfPeople > 1) numberOfPeople-- },
                            enabled = numberOfPeople > 1
                        ) {
                            Icon(Icons.Default.Remove, "Decrease")
                        }

                        Text(
                            text = "$numberOfPeople ${if (numberOfPeople == 1) "person" else "people"}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = { if (numberOfPeople < 5) numberOfPeople++ },
                            enabled = numberOfPeople < 5
                        ) {
                            Icon(Icons.Default.Add, "Increase")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Message
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Additional Information (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Any specific requirements or questions...") },
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }

            // Error Message
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (selectedDate.isEmpty()) {
                        errorMessage = "Please select a date"
                        return@Button
                    }

                    if (currentUser == null) {
                        errorMessage = "Please log in to schedule a viewing"
                        return@Button
                    }

                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null

                        try {
                            val booking = ViewingBooking(
                                id = UUID.randomUUID().toString(),
                                listingId = listingId,
                                buyerId = currentUser?.id ?: "", // Use safe call and provide default
                                sellerId = sellerId,
                                preferredDate = selectedDate,
                                preferredTime = selectedTime,
                                numberOfPeople = numberOfPeople,
                                message = message,
                                status = "pending",
                                createdAt = Timestamp.now()
                            )

                            firestore.collection("viewing_bookings")
                                .document(booking.id)
                                .set(booking)
                                .await()

                            // Send notification to seller
                            val notification = mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "userId" to sellerId,
                                "type" to "viewing_booking",
                                "title" to "New Viewing Request",
                                "message" to "Someone wants to schedule a viewing of your livestock",
                                "listingId" to listingId,
                                "isRead" to false,
                                "createdAt" to Timestamp.now()
                            )

                            firestore.collection("notifications")
                                .document(notification["id"] as String)
                                .set(notification)
                                .await()

                            isLoading = false
                            showSuccessDialog = true

                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Failed to schedule viewing: ${e.message}"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && selectedDate.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6F00)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.CalendarToday, "Schedule")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Viewing", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}