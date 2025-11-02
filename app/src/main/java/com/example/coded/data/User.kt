package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class User(
    val id: String = "",

    @PropertyName("mobile_number")
    val mobile_number: String = "",

    @PropertyName("full_name")
    val full_name: String = "",

    val email: String = "",
    val location: String = "",

    @PropertyName("profile_pic")
    val profile_pic: String = "",

    @PropertyName("token_balance")
    val token_balance: Int = 5,

    @PropertyName("free_listings_used")
    val free_listings_used: Int = 0,

    @PropertyName("free_listings_reset_date")
    val free_listings_reset_date: Timestamp = Timestamp.now(),

    @PropertyName("created_at")
    val created_at: Timestamp = Timestamp.now(),

    @PropertyName("updated_at")
    val updated_at: Timestamp = Timestamp.now(),

    @PropertyName("last_active")
    val last_active: Timestamp = Timestamp.now(),

    // ADD FCM TOKEN FIELD
    @PropertyName("fcm_token")
    val fcm_token: String = ""
) {
    // Helper method to convert to Map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "mobile_number" to mobile_number,
            "full_name" to full_name,
            "email" to email,
            "location" to location,
            "profile_pic" to profile_pic,
            "token_balance" to token_balance,
            "free_listings_used" to free_listings_used,
            "free_listings_reset_date" to free_listings_reset_date,
            "created_at" to created_at,
            "updated_at" to updated_at,
            "last_active" to last_active,
            "fcm_token" to fcm_token  // ADD THIS
        )
    }

    // Compatibility properties for existing code
    val firstName: String
        get() = full_name.split(" ").firstOrNull() ?: ""

    val lastName: String
        get() = full_name.split(" ").drop(1).joinToString(" ")

    val phone: String
        get() = mobile_number

    val profilePic: String
        get() = profile_pic

    val tokenBalance: Int
        get() = token_balance

    val freeListingsUsed: Int
        get() = free_listings_used
}