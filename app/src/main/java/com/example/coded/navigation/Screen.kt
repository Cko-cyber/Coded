package com.example.coded.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object CreateListing : Screen("create_listing")
    object Profile : Screen("profile")
    object Messages : Screen("messages")
    object MainHome : Screen("main_home") // NEW: Main screen with bottom nav
    object Listings : Screen("listings")
    object SingleStock : Screen("single_stock")
}