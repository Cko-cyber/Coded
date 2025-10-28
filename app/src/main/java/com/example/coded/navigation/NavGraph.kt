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
        // REMOVED: Splash screen - no longer needed
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

        composable(Screen.Messages.route) {
            MessagesScreen(navController, authRepository) // NOW WORKS - already imported
        }

        composable(
            route = "${Screen.SingleStock.route}/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            SingleStockScreen(navController, listingId, authRepository)
        }
        // Add these composables to your NavGraph
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
    }
}