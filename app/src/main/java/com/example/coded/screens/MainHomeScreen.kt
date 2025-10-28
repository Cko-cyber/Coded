package com.example.coded.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.coded.data.AuthRepository
import com.example.coded.data.Listing
import com.example.coded.viewmodels.ListingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainHomeScreen(navController: NavController, authRepository: AuthRepository) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val listingsViewModel: ListingsViewModel = viewModel()
    val allListings by listingsViewModel.listings.collectAsState()

    // Load listings when screen opens
    LaunchedEffect(Unit) {
        listingsViewModel.loadListings()
    }

    // Filter premium listings
    val premiumListings = remember(allListings) {
        allListings.filter { it.listingTier == "PREMIUM" && it.is_active }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herdmat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF013B33),
                    titleContentColor = Color.White
                ),
                actions = {
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
                contentColor = Color(0xFF013B33)
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
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Support, contentDescription = "Support") },
                    label = { Text("Support") },
                    selected = currentRoute == "support",
                    onClick = {
                        navController.navigate("faqs")
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Listings",
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    label = { Text("Listings") },
                    selected = currentRoute == Screen.Listings.route,
                    onClick = {
                        navController.navigate(Screen.Listings.route)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6F00),
                        selectedTextColor = Color(0xFFFF6F00),
                        indicatorColor = Color(0xFFFF6F00).copy(alpha = 0.1f),
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
                        selectedIconColor = Color(0xFF013B33),
                        selectedTextColor = Color(0xFF013B33),
                        indicatorColor = Color(0xFF013B33).copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        authRepository.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Red,
                        selectedTextColor = Color.Red,
                        indicatorColor = Color.Red.copy(alpha = 0.1f),
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
            HomeContent(navController, authRepository, premiumListings)
        }
    }
}

@Composable
fun HomeContent(
    navController: NavController,
    authRepository: AuthRepository,
    premiumListings: List<Listing>
) {
    val currentUser by authRepository.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome message
        Text(
            text = "Welcome, ${currentUser?.full_name ?: "User"}!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF013B33)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Premium Listings Carousel
        if (premiumListings.isNotEmpty()) {
            Text(
                text = "Premium Listings",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF013B33)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PremiumListingsCarousel(premiumListings, navController)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate(Screen.CreateListing.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF013B33)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Listing")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Listings.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse All Listings")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF013B33))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tokens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${currentUser?.token_balance ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Free Listings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${3 - (currentUser?.free_listings_used ?: 0)} left",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumListingsCarousel(premiumListings: List<Listing>, navController: NavController) {
    // Option 1: Using HorizontalPager (with experimental annotation)
    if (premiumListings.isNotEmpty()) {
        HorizontalPagerCarousel(premiumListings, navController)
    }
}

// Option 1: Using HorizontalPager (Experimental API)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerCarousel(premiumListings: List<Listing>, navController: NavController) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { premiumListings.size }
    )

    // Auto-scroll functionality
    LaunchedEffect(pagerState.currentPage) {
        if (premiumListings.size > 1) {
            delay(5000) // 5 seconds delay
            val nextPage = (pagerState.currentPage + 1) % premiumListings.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val listing = premiumListings[page]
            PremiumListingCard(listing, navController)
        }

        // Page indicators
        if (premiumListings.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(premiumListings.size) { index ->
                    val color = if (pagerState.currentPage == index) Color(0xFF013B33) else Color.Gray.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }
        }
    }
}

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