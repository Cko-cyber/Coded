package com.example.coded.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.coded.ui.theme.*
import com.example.coded.viewmodels.ListingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainHomeScreen(navController: NavController, authRepository: AuthRepository) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val listingsViewModel: ListingsViewModel = viewModel()
    val allListings by listingsViewModel.listings.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()
    val firestore = FirebaseFirestore.getInstance()

    var unreadNotificationsCount by remember { mutableStateOf(0) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Listen for unread notifications count
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            listenerRegistration = firestore.collection("notifications")
                .whereEqualTo("userId", currentUser?.id ?: "")
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, _ ->
                    unreadNotificationsCount = snapshot?.size() ?: 0
                }
        }
    }

    // Cleanup listener
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Load listings when screen opens
    LaunchedEffect(Unit) {
        listingsViewModel.loadListings()
    }

    // Filter premium listings
    val premiumListings = remember(allListings) {
        allListings.filter { it.listingTier == "PREMIUM" && it.is_active }
    }

    // Logout function
    fun logout() {
        FirebaseAuth.getInstance().signOut()
        authRepository.signOut()
        navController.navigate(Screen.Login.route) {
            popUpTo(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herdmat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HerdmatDeepGreen,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Notification bell with badge
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(
                                    containerColor = WarningOrange,
                                    contentColor = Color.White
                                ) {
                                    Text("$unreadNotificationsCount")
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = {
                            navController.navigate("notifications")
                        }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }

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
                contentColor = HerdmatDeepGreen
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
                        selectedIconColor = HerdmatDeepGreen,
                        selectedTextColor = HerdmatDeepGreen,
                        indicatorColor = HerdmatDeepGreen.copy(alpha = 0.1f),
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
                        selectedIconColor = HerdmatDeepGreen,
                        selectedTextColor = HerdmatDeepGreen,
                        indicatorColor = HerdmatDeepGreen.copy(alpha = 0.1f),
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
                        selectedIconColor = WarningOrange,
                        selectedTextColor = WarningOrange,
                        indicatorColor = WarningOrange.copy(alpha = 0.1f),
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
                        selectedIconColor = HerdmatDeepGreen,
                        selectedTextColor = HerdmatDeepGreen,
                        indicatorColor = HerdmatDeepGreen.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        showLogoutDialog = true
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ErrorRed,
                        selectedTextColor = ErrorRed,
                        indicatorColor = ErrorRed.copy(alpha = 0.1f),
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
            ModernHomeContent(navController, authRepository, premiumListings)
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        logout()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
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

@Composable
fun ModernHomeContent(
    navController: NavController,
    authRepository: AuthRepository,
    premiumListings: List<Listing>
) {
    val currentUser by authRepository.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(16.dp)
    ) {
        // Modern welcome section
        HerdmatCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral700
                    )
                    Text(
                        text = currentUser?.full_name ?: "User",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary900,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Profile picture with modern styling
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Primary300
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Primary700,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats cards with modern design
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Tokens",
                value = "${currentUser?.token_balance ?: 0}",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Free Listings",
                value = "${3 - (currentUser?.free_listings_used ?: 0)}",
                icon = Icons.Default.ListAlt,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Premium Listings Carousel
        if (premiumListings.isNotEmpty()) {
            Text(
                text = "Premium Listings",
                style = MaterialTheme.typography.titleLarge,
                color = Primary900,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            PremiumListingsCarousel(premiumListings, navController)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Actions
        HerdmatCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = null
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    color = Primary900,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate(Screen.CreateListing.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary700
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Listing")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Listings.route) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse All Listings")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    HerdmatCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary700,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Neutral700
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Primary900,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Keep all the existing PremiumListingsCarousel, HorizontalPagerCarousel, and PremiumListingCard functions
// They should work as-is with the updated color references

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumListingCard(
    listing: Listing,
    navController: NavController,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Card(
        modifier = modifier
            .padding(horizontal = 8.dp),
        onClick = {
            // DEBUG: Print the listing details to see what's happening
            println("Premium Listing Clicked:")
            println("  - ID: ${listing.id}")
            println("  - Breed: ${listing.breed}")
            println("  - User ID: ${listing.user_id}")
            println("  - Image URLs: ${listing.image_urls}")

            // Navigate to SingleStockScreen with the listing ID
            navController.navigate("${Screen.SingleStock.route}/${listing.id}")
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image section - using AsyncImage to load from URL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (listing.image_urls.isNotEmpty() && listing.image_urls.first().isNotEmpty()) {
                    AsyncImage(
                        model = listing.image_urls.first(),
                        contentDescription = "Listing Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback when no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = "No Image",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Image",
                                color = Color.Gray
                            )
                        }
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

            // Details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = listing.breed,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "E ${listing.price}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
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
}