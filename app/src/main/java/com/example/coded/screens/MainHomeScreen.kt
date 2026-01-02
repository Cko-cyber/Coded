package com.example.coded.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.coded.data.AuthRepository
import com.example.coded.data.Listing
import com.example.coded.ui.components.HerdmatCard
import com.example.coded.viewmodels.ListingsUiState
import com.example.coded.viewmodels.ListingsViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainHomeScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val viewModel: ListingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Real-time notification counts
    var unreadMessageCount by remember { mutableStateOf(0) }
    var unreadNotificationCount by remember { mutableStateOf(0) }

    val firestore = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    // Real-time listener for unread messages
    DisposableEffect(currentUser?.id) {
        var messageListener: com.google.firebase.firestore.ListenerRegistration? = null

        currentUser?.id?.let { userId ->
            messageListener = firestore.collection("conversations")
                .whereArrayContains("participants", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        var totalUnread = 0
                        snapshot.documents.forEach { doc ->
                            val unreadMap = doc.get("unreadCount") as? Map<String, Long>
                            totalUnread += (unreadMap?.get(userId)?.toInt() ?: 0)
                        }
                        unreadMessageCount = totalUnread
                    }
                }
        }

        onDispose {
            messageListener?.remove()
        }
    }

    // Real-time listener for unread notifications
    DisposableEffect(currentUser?.id) {
        var notificationListener: com.google.firebase.firestore.ListenerRegistration? = null

        currentUser?.id?.let { userId ->
            notificationListener = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        unreadNotificationCount = snapshot.documents.size
                    }
                }
        }

        onDispose {
            notificationListener?.remove()
        }
    }

    // Auto-refresh listings every time screen comes to foreground
    LaunchedEffect(Unit) {
        viewModel.loadListings()
    }

    // Get current route for bottom nav highlighting
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
                    // Notifications icon with badge
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFFFF6F00)
                                ) {
                                    Text("3", color = Color.White)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                        }
                    }

                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                // Home
                NavigationBarItem(
                    selected = currentRoute == "main_home",
                    onClick = { navController.navigate("main_home") },
                    icon = {
                        Icon(
                            if (currentRoute == "main_home") Icons.Filled.Home else Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f)
                    )
                )

                // Listings
                NavigationBarItem(
                    selected = currentRoute == "listings",
                    onClick = { navController.navigate("listings") },
                    icon = {
                        Icon(
                            if (currentRoute == "listings") Icons.Filled.List else Icons.Default.List,
                            contentDescription = "Listings"
                        )
                    },
                    label = { Text("Browse") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f)
                    )
                )

                // Create Listing (Center FAB-style)
                NavigationBarItem(
                    selected = currentRoute == "create_listing",
                    onClick = { navController.navigate("create_listing") },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .offset(y = (-8).dp)
                                .background(Color(0xFFFF6F00), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    label = { Text("Create") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6F00),
                        selectedTextColor = Color(0xFFFF6F00),
                        indicatorColor = Color.Transparent
                    )
                )

                // Messages
                NavigationBarItem(
                    selected = currentRoute == "messages",
                    onClick = { navController.navigate("messages") },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFFFF6F00)
                                ) {
                                    Text("2", color = Color.White)
                                }
                            }
                        ) {
                            Icon(
                                if (currentRoute == "messages") Icons.Filled.Chat else Icons.Default.Chat,
                                contentDescription = "Messages"
                            )
                        }
                    },
                    label = { Text("Messages") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f)
                    )
                )

                // Profile
                NavigationBarItem(
                    selected = currentRoute == "profile",
                    onClick = { navController.navigate("profile") },
                    icon = {
                        Icon(
                            if (currentRoute == "profile") Icons.Filled.Person else Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f)
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (uiState) {
            is ListingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF013B33))
                }
            }
            is ListingsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No listings available yet",
                            color = Color.Gray,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate("create_listing") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF013B33))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create First Listing")
                        }
                    }
                }
            }
            is ListingsUiState.Error -> {
                val errorMessage = (uiState as ListingsUiState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadListings() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF013B33))
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ListingsUiState.Success -> {
                val listings = (uiState as ListingsUiState.Success).listings
                val premiumListings = listings.filter { listing ->
                    listing.listingTier == "PREMIUM" && listing.is_active
                }
                val regularListings = listings.filter { listing ->
                    listing.listingTier != "PREMIUM" || !listing.is_active
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5))
                ) {
                    // Welcome Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF013B33),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Welcome back, ${currentUser?.full_name?.split(" ")?.first() ?: "User"}!",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Find the perfect livestock for your farm",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Premium carousel at top
                    if (premiumListings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Featured Listings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFF013B33)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PremiumListingsCarousel(premiumListings, navController)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Regular listings
                    if (regularListings.isNotEmpty()) {
                        Text(
                            text = "All Listings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFF013B33)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(regularListings) { listing ->
                                HerdmatCard(listing = listing, navController = navController)
                            }
                        }
                    } else if (premiumListings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No listings available", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFF013B33),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        authRepository.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumListingsCarousel(
    listings: List<Listing>,
    navController: NavController
) {
    val pagerState = rememberPagerState(pageCount = { listings.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp
        ) { page ->
            PremiumListingCard(listing = listings[page], navController = navController)
        }

        // Page indicators
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(listings.size) { index ->
                val color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.4f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumListingCard(listing: Listing, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("single_stock/${listing.id}") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box {
            // Image
            if (listing.image_urls.isNotEmpty()) {
                AsyncImage(
                    model = listing.image_urls.firstOrNull(),
                    contentDescription = "Premium Listing",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                }
            }

            // Premium badge
            Surface(
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 8.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "PREMIUM",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = listing.breed,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listing.location,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.getDisplayPrice(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF013B33),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Age: ${listing.age}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}