package com.example.coded.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coded.data.AuthRepository
import com.example.coded.viewmodels.SingleStockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleStockScreen(navController: NavController, listingId: String, authRepository: AuthRepository) {
    val viewModel: SingleStockViewModel = viewModel()
    val currentUser by authRepository.currentUser.collectAsState()
    val listing by viewModel.listing.collectAsState()
    val seller by viewModel.seller.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Load listing when screen opens or listingId changes
    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = listing?.breed ?: "Stock Details",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    if (currentUser?.id == listing?.user_id) {
                        IconButton(onClick = {
                            navController.navigate("edit_listing/$listingId")
                        }) {
                            Icon(Icons.Default.Edit, "Edit Listing")
                        }
                    }
                    IconButton(onClick = {
                        // TODO: Implement addToShortlist functionality
                    }) {
                        Icon(Icons.Default.Favorite, "Add to Shortlist")
                    }
                }
            )
        },
        bottomBar = {
            if (currentUser?.id != listing?.user_id && listing != null) {
                Surface(
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // TODO: Implement addToShortlist functionality
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Favorite, "Shortlist")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save")
                        }

                        Button(
                            onClick = {
                                navController.navigate("messages?listingId=$listingId&sellerId=${listing!!.user_id}")
                            },
                            modifier = Modifier.weight(2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF013B33)
                            )
                        ) {
                            Icon(Icons.Default.Chat, "Contact")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Contact Seller")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF013B33))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading listing details...")
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadListing(listingId) }
                    ) {
                        Text("Try Again")
                    }
                }
            }
        } else if (listing == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Listing not found")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Gallery
                if (listing!!.image_urls.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        AsyncImage(
                            model = listing!!.image_urls.first(),
                            contentDescription = "Listing image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Image counter if multiple images
                        if (listing!!.image_urls.size > 1) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "1/${listing!!.image_urls.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    // Placeholder when no images
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No images available")
                        }
                    }
                }

                // Details Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Tier and Status - FIXED: Use getTierEnum() to convert String to ListingTier
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tierEnum = listing!!.getTierEnum() // Convert String to ListingTier
                        Surface(
                            color = when (tierEnum) {
                                com.example.coded.data.ListingTier.FREE -> Color(0xFF4CAF50)
                                com.example.coded.data.ListingTier.BASIC -> Color(0xFF2196F3)
                                com.example.coded.data.ListingTier.BULK -> Color(0xFFFF6F00)
                                com.example.coded.data.ListingTier.PREMIUM -> Color(0xFFFFD700)
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = tierEnum.displayName, // Now works with enum
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = when (tierEnum) {
                                    com.example.coded.data.ListingTier.PREMIUM -> Color.Black
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Listing status
                        Surface(
                            color = if (listing!!.is_active) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (listing!!.is_active) "ACTIVE" else "INACTIVE",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Breed and Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listing!!.breed,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "E ${listing!!.price}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key Details Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailCard(
                            title = "Age",
                            value = listing!!.age,
                            modifier = Modifier.weight(1f)
                        )
                        DetailCard(
                            title = "Location",
                            value = listing!!.location,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seller Information
                    seller?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Seller Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF013B33)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${it.fullName}")
                                if (it.mobileNumber.isNotEmpty()) {
                                    Text("Phone: ${it.mobileNumber}")
                                }
                                if (it.location.isNotEmpty()) {
                                    Text("Location: ${it.location}")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Health Information
                    if (listing!!.vaccination_status.isNotEmpty() || listing!!.deworming.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Health Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF013B33)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (listing!!.vaccination_status.isNotEmpty()) {
                                    DetailRow(
                                        icon = Icons.Default.MedicalServices,
                                        title = "Vaccination Status",
                                        value = listing!!.vaccination_status
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (listing!!.deworming.isNotEmpty()) {
                                    DetailRow(
                                        icon = Icons.Default.HealthAndSafety,
                                        title = "Deworming Status",
                                        value = listing!!.deworming
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Full Description
                    if (listing!!.full_details.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF013B33)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = listing!!.full_details,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    // Listing Metadata
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Listed on ${listing!!.created_at.toDate().toString().substring(0, 10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DetailCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF013B33)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}