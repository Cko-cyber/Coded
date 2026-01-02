// File: app/src/main/java/com/example/coded/screens/RoleSelectionScreen.kt
package com.example.coded.screens

import androidx.compose.foundation.layout.*
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
import com.example.coded.data.HerdmatAuthRepository
import com.example.coded.data.models.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    navController: NavController,
    authRepository: HerdmatAuthRepository
) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Herdmat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Message
            Text(
                text = "Welcome to Herdmat",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF013B33)
            )

            Text(
                text = "Managed Dispatch Service Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            // Role Selection Cards
            RoleCard(
                role = UserRole.CLIENT,
                icon = Icons.Default.Person,
                title = "I Need a Service",
                description = "Create service requests. Pay securely through escrow.",
                isSelected = selectedRole == UserRole.CLIENT,
                onClick = { selectedRole = UserRole.CLIENT }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                role = UserRole.PROVIDER,
                icon = Icons.Default.Work,
                title = "I Provide Services",
                description = "Find jobs in your area. Get paid through secure escrow.",
                isSelected = selectedRole == UserRole.PROVIDER,
                onClick = { selectedRole = UserRole.PROVIDER }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                role = UserRole.ADMIN,
                icon = Icons.Default.Security,
                title = "Platform Administrator",
                description = "Manage platform operations and disputes.",
                isSelected = selectedRole == UserRole.ADMIN,
                onClick = { selectedRole = UserRole.ADMIN }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Continue Button
            Button(
                onClick = {
                    when (selectedRole) {
                        UserRole.CLIENT -> navController.navigate("client_auth")
                        UserRole.PROVIDER -> navController.navigate("provider_auth")
                        UserRole.ADMIN -> navController.navigate("admin_auth")
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedRole != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue as ${selectedRole?.name ?: ""}")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Divider()

            Spacer(modifier = Modifier.height(24.dp))

            // Guest/Anonymous Options
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Options",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )

                OutlinedButton(
                    onClick = {
                        // Start anonymous session
                        authRepository.startAnonymousSession()
                        navController.navigate("create_job") {
                            popUpTo("role_selection") { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Quickreply, "Quick Request")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Anonymous Request")
                }

                TextButton(
                    onClick = {
                        authRepository.enterGuestMode()
                        navController.navigate("jobs") {
                            popUpTo("role_selection") { inclusive = true }
                        }
                    }
                ) {
                    Text("Browse as Guest")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login link for existing users
            TextButton(
                onClick = { navController.navigate("login") }
            ) {
                Text("Already have an account? Sign In")
            }
        }
    }
}

@Composable
fun RoleCard(
    role: UserRole,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (role) {
        UserRole.CLIENT -> if (isSelected) Color(0xFFE3F2FD) else Color.White
        UserRole.PROVIDER -> if (isSelected) Color(0xFFE8F5E9) else Color.White
        UserRole.ADMIN -> if (isSelected) Color(0xFFFFF8E1) else Color.White
        else -> Color.White
    }

    val borderColor = when (role) {
        UserRole.CLIENT -> Color(0xFF2196F3)
        UserRole.PROVIDER -> Color(0xFF4CAF50)
        UserRole.ADMIN -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder(
            border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
        ) else null,
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = borderColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = borderColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}