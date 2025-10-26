// User.kt
package com.example.coded.data

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val mobile_number: String = "",
    val full_name: String = "",
    val email: String = "",
    val token_balance: Int = 0,
    val free_listings_used: Int = 0,
    val created_at: Timestamp = Timestamp.now(),
    val updated_at: Timestamp = Timestamp.now()
) {
    // Helper properties for compatibility
    val firstName: String
        get() = full_name.split(" ").firstOrNull() ?: ""

    val lastName: String
        get() = full_name.split(" ").getOrNull(1) ?: ""

    val phone: String
        get() = mobile_number

    val location: String
        get() = "" // Add location field if needed

    val profilePic: String
        get() = "" // Add profile picture field if needed
}