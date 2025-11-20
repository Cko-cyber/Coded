package com.example.coded.screens

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object MainHome : Screen("main_home")
    object Listings : Screen("listings")
    object CreateListing : Screen("create_listing")
    object Profile : Screen("profile")
    object Messages : Screen("messages")
    object SingleStock : Screen("single_stock")
}