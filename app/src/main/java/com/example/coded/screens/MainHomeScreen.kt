package com.example.coded.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.coded.data.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(navController: NavController, authRepository: AuthRepository) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herdmat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF013B33)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == Screen.MainHome.route,
                    onClick = {
                        navController.navigate(Screen.MainHome.route) {
                            popUpTo(Screen.MainHome.route) { inclusive = true }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Listings") },
                    label = { Text("Listings") },
                    selected = currentRoute == Screen.Listings.route,
                    onClick = {
                        navController.navigate(Screen.Listings.route)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create",
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    label = { Text("Create") },
                    selected = currentRoute == Screen.CreateListing.route,
                    onClick = {
                        navController.navigate(Screen.CreateListing.route)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6F00),
                        selectedTextColor = Color(0xFFFF6F00),
                        indicatorColor = Color(0xFFFF6F00).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Messages") },
                    label = { Text("Messages") },
                    selected = currentRoute == Screen.Messages.route,
                    onClick = {
                        navController.navigate(Screen.Messages.route)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = currentRoute == Screen.Profile.route,
                    onClick = {
                        navController.navigate(Screen.Profile.route)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Home content goes here
            HomeContent(navController, authRepository)
        }
    }
}

@Composable
fun HomeContent(navController: NavController, authRepository: AuthRepository) {
    val currentUser by authRepository.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome message - using full_name instead of firstName
        Text(
            text = "Welcome, ${currentUser?.fullName ?: "User"}!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF013B33)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Featured Listings or other content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate(Screen.CreateListing.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF013B33)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Listing")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Listings.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Listings")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User stats card - FIXED: using snake_case field names
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF013B33))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tokens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${currentUser?.token_balance ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Free Listings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${3 - (currentUser?.free_listings_used ?: 0)} left",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}