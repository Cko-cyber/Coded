package com.example.coded.screens.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coded.data.Job
import com.example.coded.data.JobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

/**
 * Suggested Jobs Screen - Provider views available jobs from Firebase
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestedJobsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobRepository = remember { JobRepository(context) }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var availableJobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load available jobs
    LaunchedEffect(Unit) {
        loadAvailableJobs(jobRepository, scope) { result ->
            availableJobs = result
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Jobs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32)
                ),
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            // Create test jobs if none exist
                            if (availableJobs.isEmpty()) {
                                val result = jobRepository.createTestJobsForProviders(3)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Created 3 test jobs!", Toast.LENGTH_SHORT).show()
                                    loadAvailableJobs(jobRepository, scope) { jobs ->
                                        availableJobs = jobs
                                    }
                                } else {
                                    errorMessage = "Failed to create test jobs"
                                }
                            } else {
                                // Refresh jobs
                                loadAvailableJobs(jobRepository, scope) { jobs ->
                                    availableJobs = jobs
                                    Toast.makeText(context, "Refreshed ${jobs.size} jobs", Toast.LENGTH_SHORT).show()
                                }
                            }
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val result = jobRepository.createTestJobsForProviders(2)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Added 2 test jobs!", Toast.LENGTH_SHORT).show()
                            loadAvailableJobs(jobRepository, scope) { jobs ->
                                availableJobs = jobs
                            }
                        } else {
                            errorMessage = "Failed to create test jobs"
                        }
                        isLoading = false
                    }
                },
                containerColor = Color(0xFF2E7D32)
            ) {
                Icon(Icons.Filled.Add, "Add Test Jobs")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, "Error", tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Stats card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            availableJobs.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text("Jobs Available", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "E${availableJobs.sumOf { it.totalAmount }.toInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text("Total Value", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedFilter == "NEARBY",
                    onClick = { selectedFilter = "NEARBY" },
                    label = { Text("Nearby") }
                )
                FilterChip(
                    selected = selectedFilter == "HIGH_PAY",
                    onClick = { selectedFilter = "HIGH_PAY" },
                    label = { Text("High Pay") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (availableJobs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No jobs available yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Click the + button to create test jobs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                val result = jobRepository.createTestJobsForProviders(3)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Created 3 test jobs!", Toast.LENGTH_SHORT).show()
                                    loadAvailableJobs(jobRepository, scope) { jobs ->
                                        availableJobs = jobs
                                    }
                                }
                                isLoading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text("Generate Test Jobs")
                    }
                }
            } else {
                // Job list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(availableJobs) { job ->
                        JobCard(
                            job = job,
                            onViewDetails = {
                                // Navigate to job details
                                navController.navigate("provider/job_details/${job.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobCard(
    job: Job,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onViewDetails
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Badge(
                    containerColor = when (job.serviceType) {
                        "grass_cutting" -> Color(0xFF4CAF50)
                        "yard_clearing" -> Color(0xFF795548)
                        "gardening" -> Color(0xFF8BC34A)
                        "tree_felling" -> Color(0xFF607D8B)
                        "cleaning" -> Color(0xFF2196F3)
                        "plumbing" -> Color(0xFFFF9800)
                        "electrical" -> Color(0xFF9C27B0)
                        else -> Color(0xFF2E7D32)
                    },
                    contentColor = Color.White
                ) {
                    Text(
                        text = job.serviceType.replace("_", " ").uppercase(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = job.description.take(80) + if (job.description.length > 80) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    "Location",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = job.town ?: job.region ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Filled.Person,
                    "Client",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = job.clientName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "E${String.format("%.2f", job.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Text(
                    text = formatTimeAgo(job.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Text("View Details")
                }
            }

            // Additional job info
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, "Status", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.status.replace("_", " ").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (job.status) {
                            "assigned" -> Color(0xFF2196F3)
                            "completed" -> Color(0xFF4CAF50)
                            "cancelled" -> Color(0xFFF44336)
                            else -> Color(0xFFFF9800)
                        }
                    )
                }

                if (job.isUrgent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, "Urgent",
                            tint = Color.Red, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "URGENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper function to load jobs
private fun loadAvailableJobs(
    jobRepository: JobRepository,
    scope: CoroutineScope,
    onResult: (List<Job>) -> Unit
) {
    scope.launch {
        try {
            // Try to get test jobs first
            val result = jobRepository.getTestJobsForProviders()
            if (result.isSuccess) {
                onResult(result.getOrNull() ?: emptyList())
            } else {
                // If no test jobs, create some for testing
                val createResult = jobRepository.createTestJobsForProviders(3)
                if (createResult.isSuccess) {
                    // Try to get them again
                    val testJobsResult = jobRepository.getTestJobsForProviders()
                    if (testJobsResult.isSuccess) {
                        onResult(testJobsResult.getOrNull() ?: emptyList())
                    } else {
                        onResult(emptyList())
                    }
                } else {
                    onResult(emptyList())
                }
            }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }
}

// Helper function to format timestamp to "time ago"
private fun formatTimeAgo(timestamp: com.google.firebase.Timestamp): String {
    val now = Date()
    val jobDate = timestamp.toDate()
    val diff = now.time - jobDate.time

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            sdf.format(jobDate)
        }
    }
}