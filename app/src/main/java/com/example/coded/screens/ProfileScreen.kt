package com.example.coded.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.coded.R
import com.example.coded.data.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, authRepository: AuthRepository) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screen.MainHome.route) {
                            popUpTo(Screen.MainHome.route) {
                                inclusive = false
                            }
                        }
                    }) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Profile Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF013B33))
                        ) {
                            if (currentUser?.profilePic.isNullOrEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(R.drawable.herdmat_logo),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(currentUser?.profilePic),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // FIXED: Use full_name instead of firstName and lastName
                        Text(
                            text = currentUser?.fullName ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser?.location ?: "Location not set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser?.mobileNumber ?: "Phone not set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { navController.navigate("edit_profile") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF013B33)
                            )
                        ) {
                            Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                    }
                }
            }

            // Token Balance Card - FIXED: using token_balance instead of tokenBalance
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF013B33)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Token Balance",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currentUser?.token_balance ?: 0} Tokens",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { navController.navigate("buy_tokens") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6F00)
                            )
                        ) {
                            Icon(Icons.Default.Add, "Add")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buy")
                        }
                    }
                }
            }

            // Free Listings Card - FIXED: using free_listings_used instead of freeListingsUsed
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Free Listings This Month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currentUser?.free_listings_used ?: 0} / 3 Used",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF013B33)
                            )
                        }

                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFF6F00),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Account Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = Color.Gray
                )
            }

            // Account Options
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ProfileMenuItem(
                            icon = Icons.Default.List,
                            title = "My Listings",
                            onClick = { navController.navigate("my_listings") }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        ProfileMenuItem(
                            icon = Icons.Default.Favorite,
                            title = "Shortlisted Items",
                            onClick = { navController.navigate("shortlist") }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        ProfileMenuItem(
                            icon = Icons.Default.Chat,
                            title = "Messages",
                            onClick = { navController.navigate(Screen.Messages.route) }
                        )
                    }
                }
            }

            // Support Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Support",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = Color.Gray
                )
            }

            // Support Options
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ProfileMenuItem(
                            icon = Icons.Default.Help,
                            title = "FAQs",
                            onClick = { navController.navigate("faqs") }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        ProfileMenuItem(
                            icon = Icons.Default.Support,
                            title = "Contact Support",
                            onClick = { navController.navigate("contact_support") }
                        )
                    }
                }
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, "Logout")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF013B33),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}