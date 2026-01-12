package com.example.coded.screens.provider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.coded.ui.theme.*
import com.google.firebase.Timestamp
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
                title = {
                    Text(
                        "Available Jobs",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OasisGreen
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
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
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
                containerColor = OasisGreen,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Test Jobs")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Test Jobs")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, "Error", tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            // Stats card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = OasisGreen.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, OasisMint.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = availableJobs.size.toString(),
                        label = "Jobs Available",
                        icon = Icons.Default.Work
                    )
                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = OasisMint.copy(alpha = 0.3f)
                    )
                    StatItem(
                        value = "E${availableJobs.sumOf { it.totalAmount }.toInt()}",
                        label = "Total Value",
                        icon = Icons.Default.AttachMoney
                    )
                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = OasisMint.copy(alpha = 0.3f)
                    )
                    StatItem(
                        value = availableJobs.count { it.isUrgent }.toString(),
                        label = "Urgent",
                        icon = Icons.Default.Warning,
                        iconColor = Color(0xFFFF9800)
                    )
                }
            }

            // Filter chips
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, OasisMint.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        "Filter Jobs",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = OasisDark
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OasisGreen,
                                selectedLabelColor = Color.White,
                                containerColor = OasisGray
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = true
                        )
                        FilterChip(
                            selected = selectedFilter == "NEARBY",
                            onClick = { selectedFilter = "NEARBY" },
                            label = { Text("Nearby") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OasisGreen,
                                selectedLabelColor = Color.White,
                                containerColor = OasisGray
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = true
                        )
                        FilterChip(
                            selected = selectedFilter == "HIGH_PAY",
                            onClick = { selectedFilter = "HIGH_PAY" },
                            label = { Text("High Pay") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OasisGreen,
                                selectedLabelColor = Color.White,
                                containerColor = OasisGray
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = true
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = OasisGreen)
                        Text(
                            "Loading available jobs...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OasisGray
                        )
                    }
                }
            } else if (availableJobs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.WorkOutline,
                        contentDescription = "No jobs",
                        tint = OasisGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No jobs available yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OasisDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Click the button below to create test jobs and explore the platform",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OasisGray,
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
                            containerColor = OasisGreen
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, "Generate")
                            Text("Generate Test Jobs")
                        }
                    }
                }
            } else {
                // Apply filters
                val filteredJobs = when (selectedFilter) {
                    "NEARBY" -> availableJobs.sortedBy { 0.0 } // Placeholder fix; update Job data class to add distanceFromProvider: Double? = null
                    "HIGH_PAY" -> availableJobs.sortedByDescending { it.totalAmount }
                    else -> availableJobs
                }

                // Job list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(filteredJobs) { job ->
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
fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = OasisGreen
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OasisDark
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = OasisGray
        )
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
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, OasisMint.copy(alpha = 0.2f)),
        shape = MaterialTheme.shapes.medium,
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
                    color = OasisGreen,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
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
                        else -> OasisGreen
                    },
                    contentColor = Color.White
                ) {
                    Text(
                        text = job.serviceType.replace("_", " ").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = job.description.take(80) + if (job.description.length > 80) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = OasisDark,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location and Client row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    "Location",
                    tint = OasisGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = job.town ?: job.region ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = OasisDark
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Person,
                    "Client",
                    tint = OasisGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = job.clientName,
                    style = MaterialTheme.typography.bodySmall,
                    color = OasisDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = OasisGray.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(12.dp))

            // Payment and Time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.labelSmall,
                        color = OasisGray
                    )
                    Text(
                        text = "E${String.format("%.2f", job.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OasisGreen
                    )
                }

                Text(
                    text = formatTimeAgo(job.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = OasisGray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Additional job info
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        "Status",
                        tint = when (job.status) {
                            "assigned" -> Color(0xFF2196F3)
                            "completed" -> Color(0xFF4CAF50)
                            "cancelled" -> Color(0xFFF44336)
                            else -> Color(0xFFFF9800)
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.status.replace("_", " ").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (job.status) {
                            "assigned" -> Color(0xFF2196F3)
                            "completed" -> Color(0xFF4CAF50)
                            "cancelled" -> Color(0xFFF44336)
                            else -> Color(0xFFFF9800)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }

                if (job.isUrgent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            "Urgent",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "URGENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD32F2F),
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
private fun formatTimeAgo(timestamp: Timestamp): String {
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