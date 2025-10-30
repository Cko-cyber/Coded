package com.example.coded.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coded.data.*
import com.example.coded.viewmodels.ListingsViewModel
import kotlinx.coroutines.delay

// Add the missing ListingFilter enum
enum class ListingFilter(val displayName: String) {
    ALL("All"),
    FREE("Free"),
    BASIC("Basic"),
    BULK("Bulk"),
    PREMIUM("Premium")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

                // Listings Count with Scroll Hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredListings.size} listings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    // Scroll hint indicator
                    if (filteredListings.size > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Swipe to browse",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF013B33)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll down",
                                tint = Color(0xFF013B33),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

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
                        // OPTION 1: Using VerticalPager for SNAP SCROLLING (Full tiles only)
                        SnapScrollListings(filteredListings, navController, authRepository)

                        // OPTION 2: Using LazyColumn with enhanced UX (uncomment to use instead)
                        // EnhancedLazyColumnListings(filteredListings, navController, authRepository)
                    }
                }
            }
        }
    }
}

// OPTION 1: VerticalPager with SNAP SCROLLING - Always shows full tiles
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnapScrollListings(
    listings: List<Listing>,
    navController: NavController,
    authRepository: AuthRepository
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { listings.size }
    )

    var showScrollHint by remember { mutableStateOf(true) }

    // Auto-hide scroll hint after 3 seconds
    LaunchedEffect(showScrollHint) {
        if (showScrollHint) {
            delay(3000)
            showScrollHint = false
        }
    }

    // Hide scroll hint when user starts scrolling
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage > 0) {
            showScrollHint = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val listing = listings[page]
            EnhancedListingCard(
                listing = listing,
                onClick = {
                    navController.navigate("${Screen.SingleStock.route}/${listing.id}")
                },
                navController = navController,
                authRepository = authRepository,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Page indicators for VerticalPager
        if (listings.size > 1) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(listings.size) { index ->
                    val color = if (pagerState.currentPage == index) Color(0xFF013B33) else Color.Gray.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }
        }

        // Floating scroll hint for VerticalPager
        if (showScrollHint && listings.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Surface(
                    color = Color(0xFF013B33),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = "Swipe hint",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Swipe to browse listings",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Current position indicator for VerticalPager
        if (listings.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${listings.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// OPTION 2: LazyColumn with enhanced UX (alternative)
@Composable
fun EnhancedLazyColumnListings(
    listings: List<Listing>,
    navController: NavController,
    authRepository: AuthRepository
) {
    val listState = rememberLazyListState()
    var showScrollHint by remember { mutableStateOf(true) }

    // Auto-hide scroll hint after 3 seconds
    LaunchedEffect(showScrollHint) {
        if (showScrollHint) {
            delay(3000)
            showScrollHint = false
        }
    }

    // Hide scroll hint when user starts scrolling
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex > 0) {
            showScrollHint = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(listings) { listing ->
                EnhancedListingCard(
                    listing = listing,
                    onClick = {
                        navController.navigate("${Screen.SingleStock.route}/${listing.id}")
                    },
                    navController = navController,
                    authRepository = authRepository
                )
            }
        }

        // Floating scroll indicator (disappears after scroll or timeout)
        if (showScrollHint && listings.size > 3) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    color = Color(0xFF013B33),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scroll for more listings",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll down",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Scroll progress indicator at bottom
        if (listings.size > 3) {
            val totalItems = listings.size
            val firstVisible = listState.firstVisibleItemIndex
            val scrollProgress = if (totalItems > 0) (firstVisible.toFloat() / totalItems) else 0f

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            ) {
                // Background track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                )
                // Progress indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth(scrollProgress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(Color(0xFF013B33), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedListingCard(
    listing: Listing,
    onClick: () -> Unit,
    navController: NavController,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val isOwnListing = listing.user_id == currentUser?.id

    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Image section with multiple image indicator AND EDIT BUTTON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (listing.image_urls.isNotEmpty() && listing.image_urls.first().isNotEmpty()) {
                    AsyncImage(
                        model = listing.image_urls.first(),
                        contentDescription = "Listing Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback when no image is available
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = "No Image",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Image Available",
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Top-right corner: Multiple images indicator AND Edit button for own listings
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    // Multiple images indicator (if more than 1 image)
                    if (listing.image_urls.size > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = "Multiple images",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${listing.image_urls.size}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Edit button (only show for user's own listings)
                    if (isOwnListing) {
                        Surface(
                            color = Color(0xFF013B33).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp),
                            onClick = {
                                // Navigate to edit listing screen
                                if (listing.id != null) {
                                    navController.navigate("edit_listing/${listing.id}")
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Listing",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Details section with "View Details" call-to-action
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Breed and price row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = listing.breed,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "E ${listing.price}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location and age
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = listing.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Age: ${listing.age}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tier and CTA row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tier badge
                    Surface(
                        color = when (listing.getTierEnum()) {
                            ListingTier.FREE -> Color(0xFF4CAF50)
                            ListingTier.BASIC -> Color(0xFF2196F3)
                            ListingTier.BULK -> Color(0xFFFF6F00)
                            ListingTier.PREMIUM -> Color(0xFFFFD700)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = listing.getTierEnum().displayName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (listing.getTierEnum()) {
                                ListingTier.PREMIUM -> Color.Black
                                else -> Color.White
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // "View Details" call-to-action
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF013B33),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "View details",
                            tint = Color(0xFF013B33),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}