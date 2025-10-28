package com.example.coded.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleViewingScreen(
    navController: NavController,
    listingId: String,
    sellerId: String,
    authRepository: AuthRepository
) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf("") }
    var numberOfPeople by remember { mutableStateOf(1) }
    var specialRequirements by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val currentUser by authRepository.currentUser.collectAsState()

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
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            // Submit viewing request
                            isSubmitting = true
                            // TODO: Implement viewing submission logic
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedDate != null && selectedTime.isNotEmpty() && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF013B33)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scheduling...")
                        } else {
                            Icon(Icons.Default.Visibility, contentDescription = "Schedule Viewing")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Schedule Viewing")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Schedule a Viewing",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Arrange to see the livestock in person. The seller will confirm your viewing appointment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // Date Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Viewing Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val dates = listOf("Tomorrow", "In 2 days", "In 3 days", "This weekend")
                    Column {
                        dates.forEach { date ->
                            OutlinedButton(
                                onClick = {
                                    selectedDate = System.currentTimeMillis() + when (date) {
                                        "Tomorrow" -> 24 * 60 * 60 * 1000
                                        "In 2 days" -> 2 * 24 * 60 * 60 * 1000
                                        "In 3 days" -> 3 * 24 * 60 * 60 * 1000
                                        else -> 5 * 24 * 60 * 60 * 1000 // Weekend
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedDate != null) Color(0xFF013B33) else Color.Gray
                                )
                            ) {
                                Text(date)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Time Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Viewing Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val timeSlots = listOf(
                        "9:00 AM", "10:00 AM", "11:00 AM",
                        "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM"
                    )

                    Column {
                        timeSlots.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { time ->
                                    OutlinedButton(
                                        onClick = { selectedTime = time },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (selectedTime == time) Color.White else Color(0xFF013B33),
                                            containerColor = if (selectedTime == time) Color(0xFF013B33) else Color.Transparent
                                        )
                                    ) {
                                        Text(time)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Number of People
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Number of People",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (numberOfPeople > 1) numberOfPeople-- },
                            enabled = numberOfPeople > 1
                        ) {
                            Icon(Icons.Default.Remove, "Decrease")
                        }

                        Text(
                            text = "$numberOfPeople",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )

                        IconButton(
                            onClick = { if (numberOfPeople < 10) numberOfPeople++ }
                        ) {
                            Icon(Icons.Default.Add, "Increase")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "People",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Special Requirements
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Special Requirements (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = specialRequirements,
                        onValueChange = { specialRequirements = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Any special requirements or questions about the viewing...") },
                        singleLine = false,
                        maxLines = 4
                    )
                }
            }

            // Viewing Summary
            if (selectedDate != null && selectedTime.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Viewing Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Date: ${SimpleDateFormat("EEE, MMM d").format(Date(selectedDate!!))}")
                        Text("Time: $selectedTime")
                        Text("Number of people: $numberOfPeople")
                        if (specialRequirements.isNotEmpty()) {
                            Text("Requirements: $specialRequirements")
                        }
                    }
                }
            }
        }
    }
}