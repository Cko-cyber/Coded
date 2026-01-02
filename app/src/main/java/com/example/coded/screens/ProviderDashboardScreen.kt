package com.example.coded.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.coded.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()

    val mockTrustScore = ProviderTrustScore(
        score = 78,
        tier = TrustTier.TRUSTED,
        jobsCompleted = 23,
        cleanStreak = 3,
        maxJobValue = 20000
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TrustScoreCard(trustScore = mockTrustScore)
            }

            item {
                EarningsSummaryCard(
                    totalEarnings = 45000,
                    pendingEarnings = 8500,
                    completedJobs = mockTrustScore.jobsCompleted
                )
            }

            item {
                Text(
                    "Available Jobs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(3) { index ->
                JobCard(
                    job = ServiceJob(
                        id = "job_$index",
                        title = "Veterinary checkup needed",
                        budgetAmount = 5000,
                        location = "Manzini",
                        state = JobState.Funded
                    ),
                    onAccept = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun TrustScoreCard(trustScore: ProviderTrustScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (trustScore.tier) {
                TrustTier.ELITE -> Color(0xFFFFD700)
                TrustTier.TRUSTED -> Color(0xFF4CAF50)
                TrustTier.RESTRICTED -> Color(0xFFFF9800)
                TrustTier.PROBATION -> Color(0xFFF44336)
                TrustTier.SUSPENDED -> Color(0xFF9E9E9E)
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Trust Score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        "${trustScore.score}/100",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        trustScore.tier.name,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Jobs Done", trustScore.jobsCompleted.toString())
                StatItem("Clean Streak", "${trustScore.cleanStreak}")
                StatItem("Max Job Value", "E${trustScore.maxJobValue}")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun EarningsSummaryCard(
    totalEarnings: Long,
    pendingEarnings: Long,
    completedJobs: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Earnings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Earned", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "E$totalEarnings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("In Escrow", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "E$pendingEarnings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
fun JobCard(job: ServiceJob, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Navigate to job details */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            job.location,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text(
                    "E${job.budgetAmount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                )
            ) {
                Text("Accept Job")
            }
        }
    }
}