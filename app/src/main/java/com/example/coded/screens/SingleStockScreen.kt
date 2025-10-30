package com.example.coded.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coded.data.AuthRepository
import com.example.coded.viewmodels.SingleStockViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SingleStockScreen(
    navController: NavController,
    listingId: String,
    authRepository: AuthRepository
) {
    val viewModel: SingleStockViewModel = viewModel()
    val currentUser by authRepository.currentUser.collectAsState()
    val listing by viewModel.listing.collectAsState()
    val seller by viewModel.seller.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isInShortlist by viewModel.isInShortlist.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // State for booking options dialog
    var showBookingDialog by remember { mutableStateOf(false) }

    // Load listing when screen opens - seller is loaded automatically by the ViewModel
    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
        currentUser?.let { user ->
            viewModel.checkIfInShortlist(user.id, listingId)
        }
    }

    // Booking Options Dialog
    if (showBookingDialog) {
        BookingOptionsDialog(
            onDismiss = { showBookingDialog = false },
            onBookCall = {
                showBookingDialog = false
                // Use safe call and null check
                listing?.let {
                    navController.navigate("book_call/${listingId}/${it.user_id}")
                }
            },
            onScheduleViewing = {
                showBookingDialog = false
                // Use safe call and null check
                listing?.let {
                    navController.navigate("schedule_viewing/${listingId}/${it.user_id}")
                }
            }
        )
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
                            Icon(
                                Icons.Default.Edit,
                                "Edit Listing",
                                tint = Color.White
                            )
                        }
                    }

                    // Shortlist toggle button
                    IconButton(onClick = {
                        currentUser?.let { user ->
                            coroutineScope.launch {
                                if (isInShortlist) {
                                    viewModel.removeFromShortlist(user.id, listingId)
                                } else {
                                    viewModel.addToShortlist(user.id, listingId)
                                }
                            }
                        }
                    }) {
                        Icon(
                            if (isInShortlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Toggle Shortlist",
                            tint = if (isInShortlist) Color(0xFFFF6F00) else Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
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
                        // Primary Action Buttons - SAME SIZE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Message Button
                            Button(
                                onClick = {
                                    listing?.let {
                                        navController.navigate("chat/${it.user_id}/${it.id}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF013B33)
                                )
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "Message")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Message")
                            }

                            // Book Now Button
                            Button(
                                onClick = {
                                    showBookingDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6F00)
                                )
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "Book Now")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Book Now")
                            }
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
                        onClick = {
                            viewModel.loadListing(listingId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF013B33)
                        )
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
            // Use !! here since we've already checked listing != null
            val currentListing = listing!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Carousel
                if (currentListing.image_urls.isNotEmpty()) {
                    ImageCarousel(currentListing.image_urls)
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
                        val tierEnum = currentListing.getTierEnum()
                        Surface(
                            color = when (tierEnum) {
                                com.example.coded.data.ListingTier.FREE -> Color(0xFF4CAF50)
                                com.example.coded.data.ListingTier.BASIC -> Color(0xFF2196F3)
                                com.example.coded.data.ListingTier.BULK -> Color(0xFFFF6F00)
                                com.example.coded.data.ListingTier.PREMIUM -> Color(0xFFFFD700)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = tierEnum.displayName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = when (tierEnum) {
                                    com.example.coded.data.ListingTier.PREMIUM -> Color.Black
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = if (currentListing.is_active) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (currentListing.is_active) "ACTIVE" else "INACTIVE",
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
                            text = currentListing.breed,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "E ${currentListing.price}",
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
                            value = currentListing.age,
                            modifier = Modifier.weight(1f)
                        )
                        DetailCard(
                            title = "Location",
                            value = currentListing.location,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seller Information
                    if (seller != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = seller!!.full_name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF013B33)
                                            )
                                            // Verified badge
                                            if (seller!!.mobile_number.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = Color(0xFF4CAF50),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Verified",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = seller!!.location,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }

                                    // Seller status
                                    Surface(
                                        color = Color(0xFF013B33).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Active",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF013B33),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Seller",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Seller loading state
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF013B33),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Loading seller information...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Health Information
                    if (currentListing.vaccination_status.isNotEmpty() || currentListing.deworming.isNotEmpty()) {
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

                                if (currentListing.vaccination_status.isNotEmpty()) {
                                    DetailRow(
                                        icon = Icons.Default.MedicalServices,
                                        title = "Vaccination Status",
                                        value = currentListing.vaccination_status
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (currentListing.deworming.isNotEmpty()) {
                                    DetailRow(
                                        icon = Icons.Default.HealthAndSafety,
                                        title = "Deworming Status",
                                        value = currentListing.deworming
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Full Description
                    if (currentListing.full_details.isNotEmpty()) {
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
                                    text = currentListing.full_details,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    // Listing Metadata
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Listed on ${currentListing.created_at.toDate().toString().substring(0, 10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// Image Carousel with HorizontalPager
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarousel(imageUrls: List<String>) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { imageUrls.size }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = "Listing image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Image counter
        if (imageUrls.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(imageUrls.size) { index ->
                    val color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }
        }
    }
}

// Booking Options Dialog - REMOVED "Send Message" option
@Composable
fun BookingOptionsDialog(
    onDismiss: () -> Unit,
    onBookCall: () -> Unit,
    onScheduleViewing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Book Appointment",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF013B33)
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose how you'd like to connect with the seller:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Book a Call Option
                OutlinedButton(
                    onClick = onBookCall,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF013B33)
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Book Call")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Book a Phone Call")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Schedule Viewing Option
                OutlinedButton(
                    onClick = onScheduleViewing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF013B33)
                    )
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Schedule Viewing")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Viewing")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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