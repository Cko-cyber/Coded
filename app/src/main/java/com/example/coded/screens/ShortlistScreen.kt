package com.example.coded.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coded.data.AuthRepository
import com.example.coded.data.Listing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortlistScreen(navController: NavController, authRepository: AuthRepository) {
    val currentUser by authRepository.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var shortlistedListings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load shortlisted listings
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            coroutineScope.launch {
                try {
                    isLoading = true
                    val listings = loadShortlistedListings(currentUser!!.id)
                    shortlistedListings = listings
                    error = null
                } catch (e: Exception) {
                    error = "Failed to load shortlist: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Shortlist") },
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
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF013B33)
                    )
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isLoading = true
                                        val listings = loadShortlistedListings(currentUser!!.id)
                                        shortlistedListings = listings
                                        error = null
                                    } catch (e: Exception) {
                                        error = "Failed to load shortlist: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF013B33)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }

                shortlistedListings.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No saved listings yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start browsing and save your favorites",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate(Screen.Listings.route) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF013B33)
                            )
                        ) {
                            Text("Browse Listings")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = shortlistedListings,
                            key = { it.id }
                        ) { listing ->
                            ShortlistCard(
                                listing = listing,
                                onRemove = {
                                    coroutineScope.launch {
                                        removeFromShortlist(currentUser!!.id, listing.id)
                                        shortlistedListings = shortlistedListings.filter { it.id != listing.id }
                                    }
                                },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortlistCard(
    listing: Listing,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove from Shortlist") },
            text = { Text("Are you sure you want to remove this listing from your shortlist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    }
                ) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Image
            if (listing.image_urls.isNotEmpty()) {
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    AsyncImage(
                        model = listing.image_urls.first(),
                        contentDescription = "Listing image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Card(
                    modifier = Modifier.size(100.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = listing.breed,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Age: ${listing.age}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "E ${NumberFormat.getNumberInstance(Locale.US).format(listing.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF013B33)
                )
            }

            // Remove button - FIXED: Use Delete icon instead of FavoriteRemove
            IconButton(
                onClick = { showRemoveDialog = true }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from shortlist",
                    tint = Color.Red
                )
            }
        }
    }
}

// Helper functions
suspend fun loadShortlistedListings(userId: String): List<Listing> {
    val firestore = FirebaseFirestore.getInstance()

    // Get shortlist document IDs
    val shortlistSnapshot = firestore.collection("shortlist")
        .whereEqualTo("userId", userId)
        .get()
        .await()

    val listingIds = shortlistSnapshot.documents.mapNotNull {
        it.getString("listingId")
    }

    if (listingIds.isEmpty()) {
        return emptyList()
    }

    // Get listings
    val listings = mutableListOf<Listing>()
    for (listingId in listingIds) {
        try {
            val doc = firestore.collection("listings")
                .document(listingId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(Listing::class.java)?.copy(id = doc.id)?.let {
                    listings.add(it)
                }
            }
        } catch (e: Exception) {
            // Continue loading other listings even if one fails
            continue
        }
    }

    return listings
}

suspend fun removeFromShortlist(userId: String, listingId: String) {
    try {
        FirebaseFirestore.getInstance()
            .collection("shortlist")
            .document("${userId}_${listingId}")
            .delete()
            .await()
    } catch (e: Exception) {
        throw Exception("Failed to remove from shortlist: ${e.message}")
    }
}