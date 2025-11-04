package com.example.coded.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.coded.data.AuthRepository
import com.example.coded.screens.*

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = if (authRepository.isUserLoggedIn()) Screen.MainHome.route else Screen.Login.route
    ) {
        // Auth Flow
        composable(Screen.Login.route) {
            LoginScreen(navController, authRepository)
        }

        composable(Screen.Signup.route) {
            SignUpScreen(navController, authRepository)
        }

        // Main App Flow with Bottom Navigation
        composable(Screen.MainHome.route) {
            MainHomeScreen(navController, authRepository)
        }

        composable(Screen.Listings.route) {
            ListingsScreen(navController, authRepository)
        }

        composable(Screen.CreateListing.route) {
            CreateListingScreen(navController, authRepository)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController, authRepository)
        }

        // Messages Screen (Conversations List)
        composable(Screen.Messages.route) {
            EnhancedMessagesScreen(navController, authRepository)
        }

        composable(
            route = "${Screen.SingleStock.route}/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            SingleStockScreen(navController, listingId, authRepository)
        }

        // Edit Listing Route
        composable(
            route = "edit_listing/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            EditListingScreen(navController, listingId)
        }

        // Chat Screen - Individual Conversation
        composable(
            route = "chat/{userId}/{listingId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("listingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("userId") ?: ""
            val listingId = backStackEntry.arguments?.getString("listingId")

            EnhancedChatScreen(
                navController = navController,
                otherUserId = otherUserId,
                listingId = listingId,
                authRepository = authRepository
            )
        }

        // Book Call Screen
        composable(
            route = "book_call/{listingId}/{sellerId}",
            arguments = listOf(
                navArgument("listingId") { type = NavType.StringType },
                navArgument("sellerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            BookCallScreen(navController, listingId, sellerId, authRepository)
        }

        // Schedule Viewing Screen
        composable(
            route = "schedule_viewing/{listingId}/{sellerId}",
            arguments = listOf(
                navArgument("listingId") { type = NavType.StringType },
                navArgument("sellerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            ScheduleViewingScreen(navController, listingId, sellerId, authRepository)
        }

        // ✅ ADDED: Booking Details Screen
        composable(
            route = "booking_details/{listingId}/{bookingType}?buyerName={buyerName}&buyerId={buyerId}&preferredDate={preferredDate}&preferredTime={preferredTime}&numberOfPeople={numberOfPeople}",
            arguments = listOf(
                navArgument("listingId") { type = NavType.StringType },
                navArgument("bookingType") { type = NavType.StringType },
                navArgument("buyerName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("buyerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("preferredDate") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("preferredTime") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("numberOfPeople") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            BookingDetailsScreen(
                navController = navController,
                authRepository = authRepository,
                listingId = backStackEntry.arguments?.getString("listingId") ?: "",
                bookingType = backStackEntry.arguments?.getString("bookingType") ?: "call",
                buyerName = backStackEntry.arguments?.getString("buyerName"),
                buyerId = backStackEntry.arguments?.getString("buyerId"),
                preferredDate = backStackEntry.arguments?.getString("preferredDate"),
                preferredTime = backStackEntry.arguments?.getString("preferredTime"),
                numberOfPeople = backStackEntry.arguments?.getString("numberOfPeople")
            )
        }

        // Profile Sub-screens
        composable("buy_tokens") {
            BuyTokensScreen(navController, authRepository)
        }

        composable("edit_profile") {
            EditProfileScreen(navController, authRepository)
        }

        composable("my_listings") {
            MyListingsScreen(navController, authRepository)
        }

        composable("shortlist") {
            ShortlistScreen(navController, authRepository)
        }

        composable("faqs") {
            FAQScreen(navController)
        }

        composable("contact_support") {
            ContactSupportScreen(navController, authRepository)
        }

        // Notifications Screen
        composable("notifications") {
            NotificationsScreen(navController, authRepository)
        }
    }
}