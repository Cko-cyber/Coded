package com.example.coded.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coded.R
import androidx.compose.foundation.BorderStroke
import com.example.coded.data.OasisAuthRepository
import com.example.coded.ui.theme.OasisGreen
import com.example.coded.ui.theme.OasisMint
import com.example.coded.ui.theme.OasisDark
import com.example.coded.ui.theme.OasisGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEntryScreen(
    authRepository: OasisAuthRepository,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Welcome to Oasis",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OasisGreen
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Logo/Icon Section
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(OasisGreen.copy(alpha = 0.1f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.oasis_logo_icon),
                        contentDescription = "Oasis Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Welcome Message
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Welcome to Oasis Services",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OasisDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "On-demand services at your fingertips",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OasisGray,
                        textAlign = TextAlign.Center
                    )
                }

                // Service Options Grid
                ServiceOptionsGrid(
                    navController = navController,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Actions
                QuickActionsSection(
                    navController = navController,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceOptionsGrid(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val services = listOf(
        ServiceOption(
            id = "grass_cutting",
            name = "Grass Cutting",
            icon = Icons.Default.Grass,
            color = OasisGreen
        ),
        ServiceOption(
            id = "yard_clearing",
            name = "Yard Clearing",
            icon = Icons.Default.Yard,
            color = OasisGreen.copy(alpha = 0.8f)
        ),
        ServiceOption(
            id = "tree_felling",
            name = "Tree Felling",
            icon = Icons.Default.Park,
            color = OasisMint
        ),
        ServiceOption(
            id = "cleaning",
            name = "Cleaning",
            icon = Icons.Default.CleaningServices,
            color = OasisGray
        ),
        ServiceOption(
            id = "plumbing",
            name = "Plumbing",
            icon = Icons.Default.Plumbing,
            color = Color(0xFF2196F3)
        ),
        ServiceOption(
            id = "electrical",
            name = "Electrical",
            icon = Icons.Default.ElectricalServices,
            color = Color(0xFFFF9800)
        ),
        ServiceOption(
            id = "gardening",
            name = "Gardening",
            icon = Icons.Default.Spa,
            color = Color(0xFF4CAF50)
        ),
        ServiceOption(
            id = "view_all",
            name = "View All",
            icon = Icons.Default.MoreHoriz,
            color = OasisDark
        )
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Popular Services",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OasisDark
        )

        GridLayout(
            items = services,
            columns = 2,
            modifier = Modifier.fillMaxWidth()
        ) { service ->
            ServiceCard(
                service = service,
                onClick = {
                    if (service.id == "view_all") {
                        navController.navigate("services_list")
                    } else {
                        navController.navigate("create_job?service=${service.id}")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceCard(
    service: ServiceOption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(service.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = service.icon,
                    contentDescription = service.name,
                    tint = service.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                service.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = OasisDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSection(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OasisDark
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Create Job Button
            Button(
                onClick = { navController.navigate("create_job") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OasisGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create Job",
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Create Job")
                }
            }

            // My Jobs Button - Fixed OutlinedButton
            OutlinedButton(
                onClick = { navController.navigate("my_jobs") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OasisGreen
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Work,
                        contentDescription = "My Jobs",
                        modifier = Modifier.size(20.dp)
                    )
                    Text("My Jobs")
                }
            }
        }

        // Profile & Settings Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, OasisGreen),
                onClick = { navController.navigate("profile") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = OasisGreen
                    )
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.labelMedium,
                        color = OasisDark
                    )
                }
            }

            // Settings Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, OasisGreen),
                onClick = { navController.navigate("settings") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = OasisGreen
                    )
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = OasisDark
                    )
                }
            }
        }
    }
}

// Grid Layout composable
@Composable
fun <T> GridLayout(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Column(modifier = modifier) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        content(item)
                    }
                }
                // Add empty spacers for missing items in the last row
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Data class for service options
data class ServiceOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)