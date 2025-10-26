package com.example.coded.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coded.data.*
import com.example.coded.viewmodels.ListingsViewModel

// Add the missing ListingFilter enum
enum class ListingFilter(val displayName: String) {
    ALL("All"),
    FREE("Free"),
    BASIC("Basic"),
    BULK("Bulk"),
    PREMIUM("Premium")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingsScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val listingsViewModel: ListingsViewModel = viewModel()
    val listings by listingsViewModel.listings.collectAsState()
    val isLoading by listingsViewModel.isLoading.collectAsState()
    val error by listingsViewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ListingFilter.ALL) }

    // Filter listings based on search and filter
    val filteredListings = remember(listings, searchQuery, selectedFilter) {
        var result = listings

        // Apply search filter
        if (searchQuery.isNotBlank()) {
            result = listingsViewModel.searchListings(searchQuery)
        }

        // FIXED: Compare string values instead of enum
        if (selectedFilter != ListingFilter.ALL) {
            result = result.filter { it.listingTier == selectedFilter.name }
        }

        result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Listings") },
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
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search listings...") },
                    placeholder = { Text("Search by breed, location, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Chips
                Text(
                    text = "Filter by Tier:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF013B33)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListingFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF013B33),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Listings Count
                Text(
                    text = "Showing ${filteredListings.size} listings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Listings Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF013B33))
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
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { listingsViewModel.loadListings() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF013B33)
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }

                    filteredListings.isEmpty() -> {
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
                                text = if (searchQuery.isNotEmpty() || selectedFilter != ListingFilter.ALL) {
                                    "No listings match your search criteria"
                                } else {
                                    "No listings available"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            if (searchQuery.isNotEmpty() || selectedFilter != ListingFilter.ALL) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedFilter = ListingFilter.ALL
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF013B33)
                                    )
                                ) {
                                    Text("Clear Filters")
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredListings) { listing ->
                                ListingCard(
                                    listing = listing,
                                    onClick = {
                                        navController.navigate("${Screen.SingleStock.route}/${listing.id}")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Image section
            if (listing.image_urls.isNotEmpty()) {
                // You'll need to implement the image display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Image Preview",
                        color = Color.Gray
                    )
                }
            }

            // Details section
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = listing.breed,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                    text = "E ${listing.price}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF013B33)
                )
                // Show tier - FIXED: Use getTierEnum().displayName
                Text(
                    text = "Tier: ${listing.getTierEnum().displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}