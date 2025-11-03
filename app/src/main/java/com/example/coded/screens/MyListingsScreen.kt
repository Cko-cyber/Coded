package com.example.coded.screens

import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coded.data.AuthRepository
import com.example.coded.data.Listing
// ✅ FIXED IMPORT: adjust to where your MyListingsViewModel file actually is
import com.example.coded.screens.MyListingsViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(navController: NavController, authRepository: AuthRepository) {
    val myListingsViewModel: MyListingsViewModel = viewModel()
    val listings by myListingsViewModel.listings.collectAsState()
    val isLoading by myListingsViewModel.isLoading.collectAsState()
    val error by myListingsViewModel.error.collectAsState()

    val currentUser by authRepository.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Debug logging for state changes
    LaunchedEffect(listings) {
        println("🔄 MyListingsScreen: Listings updated - ${listings.size} items")
        listings.forEachIndexed { index, listing ->
            println("   📝 [$index] ${listing.breed} - ID: ${listing.id} - Active: ${listing.is_active}")
        }
    }

    // Add authentication check
    if (currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo("my_listings") { inclusive = true }
            }
        }
        return
    }

    // Use real-time listener when user is available
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            println("🚀 MyListingsScreen: Loading listings for user: ${currentUser!!.id}")
            myListingsViewModel.loadUserListings(currentUser!!.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    println("➕ Navigating to create listing")
                    navController.navigate("create_listing")
                },
                containerColor = Color(0xFF013B33)
            ) {
                Icon(Icons.Default.Add, "Create Listing")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF013B33))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Loading your listings...")
                            }
                        }
                    }

                    error != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                modifier = Modifier.size(64.dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                // ✅ FIXED: remove extra argument (function takes none)
                                onClick = {
                                    myListingsViewModel.refreshUserListings()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF013B33)
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }

                    listings.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = "No listings",
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "You haven't created any listings yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    println("➕ Navigating to create listing from empty state")
                                    navController.navigate("create_listing")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF013B33)
                                )
                            ) {
                                Text("Create Your First Listing")
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = listings,
                                key = { listing -> listing.id ?: "" }
                            ) { listing ->
                                MyListingCard(
                                    listing = listing,
                                    onEdit = {
                                        if (listing.id != null) {
                                            println("✏️ Navigating to edit listing: ${listing.id}")
                                            navController.navigate("edit_listing/${listing.id}")
                                        } else {
                                            println("❌ Cannot edit listing with null ID")
                                        }
                                    },
                                    onToggleActive = {
                                        coroutineScope.launch {
                                            if (listing.id != null) {
                                                println("🔄 Toggling listing ${listing.id} active state")
                                                myListingsViewModel.toggleListingActive(listing.id, !listing.is_active)
                                            } else {
                                                println("❌ Cannot toggle listing with null ID")
                                            }
                                        }
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            if (listing.id != null) {
                                                println("🗑️ Deleting listing: ${listing.id}")
                                                myListingsViewModel.deleteListing(listing.id)
                                            } else {
                                                println("❌ Cannot delete listing with null ID")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyListingCard(
    listing: Listing,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.breed,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = if (listing.is_active) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (listing.is_active) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Age: ${listing.age}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Location: ${listing.location}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "E ${NumberFormat.getNumberInstance(Locale.US).format(listing.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF013B33)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tier: ${listing.getTierEnum().displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF013B33)
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onToggleActive,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (listing.is_active) Color(0xFFFF6F00) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        if (listing.is_active) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (listing.is_active) "Deactivate" else "Activate")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
