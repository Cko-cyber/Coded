package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class User(
    val id: String = "",

    // Match Firestore field names exactly
    @PropertyName("firstName") val firstName: String = "",
    @PropertyName("lastName") val lastName: String = "",
    @PropertyName("phone") val phone: String = "",
    @PropertyName("mobile_number") val mobileNumber: String = "", // Keep for backward compatibility
    @PropertyName("profilePic") val profilePic: String = "",
    @PropertyName("location") val location: String = "",

    // Your existing fields
    @PropertyName("full_name") val fullName: String = "",
    val email: String = "",
    val token_balance: Int = 0,
    val free_listings_used: Int = 0,
    val created_at: Timestamp = Timestamp.now(),
    val updated_at: Timestamp = Timestamp.now()
) {
    // Helper property for full_name compatibility
    val displayName: String
        get() = if (fullName.isNotEmpty()) fullName else "$firstName $lastName".trim()

    // Helper for mobile_number compatibility
    val mobile: String
        get() = if (mobileNumber.isNotEmpty()) mobileNumber else phone
}