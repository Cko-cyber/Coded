// File: app/src/main/java/com/example/coded/data/models/UserProfile.kt
package com.example.coded.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class UserProfile(
    // Core Identity
    @PropertyName("id") val id: String = "",
    @PropertyName("firebase_uid") val firebaseUid: String = "", // For authenticated users
    @PropertyName("role") val role: UserRole = UserRole.GUEST,

    // Contact Information
    @PropertyName("email") val email: String = "",
    @PropertyName("phone") val phone: String = "",
    @PropertyName("full_name") val fullName: String = "",

    // Location
    @PropertyName("location") val location: String = "",
    @PropertyName("coordinates") val coordinates: Map<String, Double> = emptyMap(), // For dispatch

    // Profile Media
    @PropertyName("profile_pic_url") val profilePicUrl: String = "",
    @PropertyName("verification_docs") val verificationDocs: List<String> = emptyList(),

    // Role-Specific Fields
    @PropertyName("provider_info") val providerInfo: ProviderInfo? = null,
    @PropertyName("client_info") val clientInfo: ClientInfo? = null,
    @PropertyName("admin_info") val adminInfo: AdminInfo? = null,

    // Platform Stats
    @PropertyName("trust_score") val trustScore: Int = 70,
    @PropertyName("risk_score") val riskScore: Int = 70,
    @PropertyName("is_verified") val isVerified: Boolean = false,
    @PropertyName("is_suspended") val isSuspended: Boolean = false,

    // Timestamps
    @PropertyName("created_at") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updated_at") val updatedAt: Timestamp = Timestamp.now(),
    @PropertyName("last_active") val lastActive: Timestamp = Timestamp.now(),

    // FCM Token
    @PropertyName("fcm_token") val fcmToken: String = ""
) {
    // Role-specific data classes
    data class ProviderInfo(
        @PropertyName("skills") val skills: List<String> = emptyList(),
        @PropertyName("service_radius_km") val serviceRadiusKm: Int = 50,
        @PropertyName("hourly_rate") val hourlyRate: Double? = null,
        @PropertyName("experience_years") val experienceYears: Int = 0,
        @PropertyName("total_jobs_completed") val totalJobsCompleted: Int = 0,
        @PropertyName("avg_rating") val averageRating: Double = 0.0,
        @PropertyName("available") val isAvailable: Boolean = true,
        @PropertyName("verification_status") val verificationStatus: String = "PENDING", // PENDING, VERIFIED, REJECTED
        @PropertyName("bank_account") val bankAccount: BankAccount? = null
    )

    data class ClientInfo(
        @PropertyName("preferred_payment_method") val preferredPaymentMethod: String = "MoMo",
        @PropertyName("total_jobs_posted") val totalJobsPosted: Int = 0,
        @PropertyName("dispute_rate") val disputeRate: Double = 0.0,
        @PropertyName("requires_pre_funding") val requiresPreFunding: Boolean = false
    )

    data class AdminInfo(
        @PropertyName("admin_level") val adminLevel: Int = 1, // 1=Support, 2=Moderator, 3=SuperAdmin
        @PropertyName("permissions") val permissions: List<String> = emptyList(),
        @PropertyName("managed_region") val managedRegion: String = ""
    )

    data class BankAccount(
        @PropertyName("bank_name") val bankName: String = "",
        @PropertyName("account_number") val accountNumber: String = "",
        @PropertyName("account_name") val accountName: String = "",
        @PropertyName("branch_code") val branchCode: String = ""
    )

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "firebase_uid" to firebaseUid,
            "role" to role.name,
            "email" to email,
            "phone" to phone,
            "full_name" to fullName,
            "location" to location,
            "coordinates" to coordinates,
            "profile_pic_url" to profilePicUrl,
            "verification_docs" to verificationDocs,
            "provider_info" to (providerInfo?.toMap() ?: emptyMap<String, Any>()),
            "client_info" to (clientInfo?.toMap() ?: emptyMap<String, Any>()),
            "admin_info" to (adminInfo?.toMap() ?: emptyMap<String, Any>()),
            "trust_score" to trustScore,
            "risk_score" to riskScore,
            "is_verified" to isVerified,
            "is_suspended" to isSuspended,
            "created_at" to createdAt,
            "updated_at" to updatedAt,
            "last_active" to lastActive,
            "fcm_token" to fcmToken
        )
    }

    fun isEligibleForDispatch(): Boolean {
        return role == UserRole.PROVIDER &&
                isVerified &&
                !isSuspended &&
                providerInfo?.isAvailable == true &&
                trustScore >= 50
    }
}

// Extension functions for serialization
fun UserProfile.ProviderInfo.toMap(): Map<String, Any> {
    return mapOf(
        "skills" to skills,
        "service_radius_km" to serviceRadiusKm,
        "hourly_rate" to (hourlyRate ?: ""),
        "experience_years" to experienceYears,
        "total_jobs_completed" to totalJobsCompleted,
        "avg_rating" to averageRating,
        "available" to isAvailable,
        "verification_status" to verificationStatus,
        "bank_account" to (bankAccount?.toMap() ?: emptyMap<String, Any>())
    )
}

fun UserProfile.ClientInfo.toMap(): Map<String, Any> {
    return mapOf(
        "preferred_payment_method" to preferredPaymentMethod,
        "total_jobs_posted" to totalJobsPosted,
        "dispute_rate" to disputeRate,
        "requires_pre_funding" to requiresPreFunding
    )
}

fun UserProfile.AdminInfo.toMap(): Map<String, Any> {
    return mapOf(
        "admin_level" to adminLevel,
        "permissions" to permissions,
        "managed_region" to managedRegion
    )
}

fun UserProfile.BankAccount.toMap(): Map<String, Any> {
    return mapOf(
        "bank_name" to bankName,
        "account_number" to accountNumber,
        "account_name" to accountName,
        "branch_code" to branchCode
    )
}