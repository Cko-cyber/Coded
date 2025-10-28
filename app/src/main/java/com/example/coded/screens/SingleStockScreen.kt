package com.example.coded.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
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
    var showContactDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listingId, currentUser) {
        viewModel.loadListing(listingId)
        currentUser?.let {
            viewModel.checkIfInShortlist(it.id, listingId)
        }
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
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White
                ),
                actions = {
                    if (currentUser?.id == listing?.user_id) {
                        IconButton(onClick = {
                            navController.navigate("edit_listing/$listingId")
                        }) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.White)
                        }
                    }

                    // Functional Heart Icon - Instagram style
                    IconButton(
                        onClick = {
                            currentUser?.let { user ->
                                coroutineScope.launch {
                                    if (isInShortlist) {
                                        viewModel.removeFromShortlist(user.id, listingId)
                                    } else {
                                        viewModel.addToShortlist(user.id, listingId)
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isInShortlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isInShortlist) "Remove from shortlist" else "Add to shortlist",
                            tint = if (isInShortlist) Color.Red else Color.White
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
                    // REMOVED Save Button - Only Contact Seller
                    Button(
                        onClick = { showContactDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF013B33)
                        )
                    ) {
                        Icon(Icons.Default.Chat, "Contact")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contact Seller", style = MaterialTheme.typography.titleMedium)
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
                CircularProgressIndicator(color = Color(0xFF013B33))
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error!!)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadListing(listingId) }) {
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
                Text("Listing not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Carousel
                if (listing!!.image_urls.isNotEmpty()) {
                    val pagerState = rememberPagerState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        HorizontalPager(
                            count = listing!!.image_urls.size,
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = listing!!.image_urls[page],
                                contentDescription = "Image ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Image counter
                        if (listing!!.image_urls.size > 1) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1}/${listing!!.image_urls.size}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Pager Indicator
                            HorizontalPagerIndicator(
                                pagerState = pagerState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                activeColor = Color.White,
                                inactiveColor = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Pets, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val tierEnum = listing!!.getTierEnum()
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

                    // Details Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailCard("Age", listing!!.age, Modifier.weight(1f))
                        DetailCard("Location", listing!!.location, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seller Information
                    seller?.let {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Seller Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF013B33)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it.full_name)
                                if (it.mobile_number.isNotEmpty()) {
                                    Text("Phone: ${it.mobile_number}")
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
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Health Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF013B33)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (listing!!.vaccination_status.isNotEmpty()) {
                                    DetailRow(Icons.Default.MedicalServices, "Vaccination", listing!!.vaccination_status)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (listing!!.deworming.isNotEmpty()) {
                                    DetailRow(Icons.Default.HealthAndSafety, "Deworming", listing!!.deworming)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Description
                    if (listing!!.full_details.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF013B33)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(listing!!.full_details)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Listed on ${listing!!.created_at.toDate().toString().substring(0, 10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // Contact Seller Dialog with Multiple Options
    if (showContactDialog) {
        ContactSellerDialog(
            onDismiss = { showContactDialog = false },
            onSendMessage = {
                showContactDialog = false
                navController.navigate("chat?listingId=$listingId&sellerId=${listing!!.user_id}")
            },
            onBookCall = {
                showContactDialog = false
                // TODO: Implement book call functionality
            },
            onScheduleViewing = {
                showContactDialog = false
                // TODO: Implement schedule viewing
            }
        )
    }
}

@Composable
fun ContactSellerDialog(
    onDismiss: () -> Unit,
    onSendMessage: () -> Unit,
    onBookCall: () -> Unit,
    onScheduleViewing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact Seller", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Choose how you'd like to contact the seller:")
                Spacer(modifier = Modifier.height(16.dp))

                // Send Message
                OutlinedButton(
                    onClick = onSendMessage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Message, "Message")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Message")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Book Call
                OutlinedButton(
                    onClick = onBookCall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Phone, "Call")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Book a Call")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Schedule Viewing
                OutlinedButton(
                    onClick = onScheduleViewing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Event, "Viewing")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Viewing")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
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