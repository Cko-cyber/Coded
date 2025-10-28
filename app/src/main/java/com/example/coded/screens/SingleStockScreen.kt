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
import com.example.coded.data.ListingTier
import com.example.coded.viewmodels.SingleStockViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleStockScreen(navController: NavController, listingId: String, authRepository: AuthRepository) {
    val viewModel: SingleStockViewModel = viewModel()
    val currentUser by authRepository.currentUser.collectAsState()

    // Collect state properly - use .value for State objects
    val listingState = viewModel.listing
    val sellerState = viewModel.seller
    val isLoadingState = viewModel.isLoading
    val errorState = viewModel.error

    val listing by remember { listingState }
    val seller by remember { sellerState }
    val isLoading by remember { isLoadingState }
    val error by remember { errorState }

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
                            Icon(Icons.Default.Edit, "Edit Listing", tint = Color.White)
                        }
                    }
                    IconButton(onClick = {
                        // TODO: Implement addToShortlist functionality
                    }) {
                        Icon(Icons.Default.Favorite, "Add to Shortlist", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            // Show action buttons for ALL listings when not owner
            if (currentUser?.id != listing?.user_id && listing != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Contact Seller Button - For ALL tiers
                        Button(
                            onClick = {
                                // Navigate to messages with listing and seller info
                                navController.navigate(
                                    "messages?listingId=$listingId&sellerId=${listing!!.user_id}"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF013B33)
                            )
                        ) {
                            Icon(Icons.Default.Chat, "Contact")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Contact Seller")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Additional buttons for Premium/Basic listings
                        val tier = getTierFromListing(listing!!)
                        if (tier in listOf(ListingTier.PREMIUM, ListingTier.BASIC)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        navController.navigate(
                                            "messages?listingId=$listingId&sellerId=${listing!!.user_id}"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Phone, "Call", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Book Call")
                                }

                                OutlinedButton(
                                    onClick = {
                                        navController.navigate(
                                            "messages?listingId=$listingId&sellerId=${listing!!.user_id}"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Visibility, "View", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Book Viewing")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Save to Shortlist
                        OutlinedButton(
                            onClick = {
                                // TODO: Implement shortlist
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Favorite, "Shortlist")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Shortlist")
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
                val imageUrls = listing!!.image_urls ?: emptyList()
                if (imageUrls.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        AsyncImage(
                            model = imageUrls.first(),
                            contentDescription = "Listing image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (imageUrls.size > 1) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "1/${imageUrls.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
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
                    // Tier and Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tier = getTierFromListing(listing!!)
                        Surface(
                            color = when (tier) {
                                ListingTier.FREE -> Color(0xFF4CAF50)
                                ListingTier.BASIC -> Color(0xFF2196F3)
                                ListingTier.BULK -> Color(0xFFFF6F00)
                                ListingTier.PREMIUM -> Color(0xFFFFD700)
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = tier.displayName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = when (tier) {
                                    ListingTier.PREMIUM -> Color.Black
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val isActive = listing!!.is_active ?: false
                        Surface(
                            color = if (isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isActive) "ACTIVE" else "INACTIVE",
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
                            text = listing!!.breed ?: "Unknown Breed",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "KSh ${listing!!.price ?: 0}",
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
                            value = listing!!.age ?: "Unknown",
                            modifier = Modifier.weight(1f)
                        )
                        DetailCard(
                            title = "Location",
                            value = listing!!.location ?: "Unknown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seller Information
                    seller?.let { currentSeller ->
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
                                Text(currentSeller.full_name ?: "Unknown Seller")
                                if (!currentSeller.mobile_number.isNullOrEmpty()) {
                                    Text("Phone: ${currentSeller.mobile_number}")
                                }
                                if (!currentSeller.location.isNullOrEmpty()) {
                                    Text("Location: ${currentSeller.location}")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Health Information
                    val vaccinationStatus = listing!!.vaccination_status ?: ""
                    val dewormingStatus = listing!!.deworming ?: ""

                    if (vaccinationStatus.isNotEmpty() || dewormingStatus.isNotEmpty()) {
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

                                if (vaccinationStatus.isNotEmpty()) {
                                    DetailRow(
                                        icon = Icons.Default.MedicalServices,
                                        title = "Vaccination Status",
                                        value = vaccinationStatus
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (dewormingStatus.isNotEmpty()) {
                                    DetailRow(
                                        icon = Icons.Default.HealthAndSafety,
                                        title = "Deworming Status",
                                        value = dewormingStatus
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Full Description
                    val fullDetails = listing!!.full_details ?: ""
                    if (fullDetails.isNotEmpty()) {
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
                                    text = fullDetails,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    // Listing Metadata
                    Spacer(modifier = Modifier.height(16.dp))
                    val createdAt = listing!!.created_at ?: Timestamp.now()
                    val dateString = formatTimestamp(createdAt)
                    Text(
                        text = "Listed on $dateString",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // Extra space for bottom bar
                    Spacer(modifier = Modifier.height(160.dp))
                }
            }
        }
    }
}

// Helper function to get tier from listing
private fun getTierFromListing(listing: com.example.coded.data.Listing): ListingTier {
    return when (listing.listingTier.uppercase()) {
        "FREE" -> ListingTier.FREE
        "BASIC" -> ListingTier.BASIC
        "BULK" -> ListingTier.BULK
        "PREMIUM" -> ListingTier.PREMIUM
        else -> ListingTier.FREE
    }
}

// Helper function to format timestamp
private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
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