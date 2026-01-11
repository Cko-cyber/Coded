package com.example.coded.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp

// ==================== JOB MODELS ====================

data class ServiceJob(
    val id: String = "",
    val clientId: String = "",
    val providerId: String? = null,
    val transactionId: String = "",
    val serviceType: String = "",
    val description: String = "",
    val location: JobLocation = JobLocation(),
    val area: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val estimatedPrice: Double = 0.0,
    val finalPrice: Double = 0.0,
    val escrowAmount: Double = 0.0,
    val state: JobState = JobState.Created,
    val stateHistory: List<StateTransition> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val scheduledTime: Long? = null,
    val completedAt: Long? = null,
    val rating: Int? = null,
    val review: String? = null
)

data class JobLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val region: String = ""
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)

    companion object {
        fun fromLatLng(latLng: LatLng, address: String = ""): JobLocation {
            return JobLocation(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                address = address
            )
        }
    }
}

sealed class JobState {
    object Created : JobState()
    object Funded : JobState()
    object Assigned : JobState()
    object Accepted : JobState()
    object InProgress : JobState()
    object Completed : JobState()
    data class Verified(val autoVerified: Boolean = false) : JobState()
    object PaidOut : JobState()
    object Cancelled : JobState()
    object Disputed : JobState()

    fun toDisplayString(): String = when (this) {
        is Created -> "Draft"
        is Funded -> "Active"
        is Assigned -> "Assigned"
        is Accepted -> "Accepted"
        is InProgress -> "In Progress"
        is Completed -> "Completed"
        is Verified -> if (autoVerified) "Auto-Verified" else "Verified"
        is PaidOut -> "Paid Out"
        is Cancelled -> "Cancelled"
        is Disputed -> "Disputed"
    }
}

data class StateTransition(
    val from: JobState,
    val to: JobState,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)

// ==================== USER MODELS ====================

data class AnonymousClient(
    val transactionId: String,
    val deviceHash: String?,
    val riskScore: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val jobsCompleted: Int = 0
)

data class Provider(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null,
    val serviceTypes: List<String> = emptyList(),
    val location: ProviderLocation? = null,
    val rating: Double = 0.0,
    val totalJobs: Int = 0,
    val verified: Boolean = false,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val skills: List<String> = emptyList(),
    val bio: String? = null
)

data class ProviderLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "",
    val region: String = ""
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

data class Admin(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: AdminRole = AdminRole.MODERATOR,
    val permissions: List<AdminPermission> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class AdminRole {
    SUPER_ADMIN,
    ADMIN,
    MODERATOR
}

enum class AdminPermission {
    MANAGE_JOBS,
    MANAGE_USERS,
    MANAGE_PROVIDERS,
    MANAGE_PAYMENTS,
    VERIFY_JOBS,
    VIEW_ANALYTICS,
    SEND_NOTIFICATIONS
}

// ==================== PAYMENT MODELS ====================

data class Transaction(
    val id: String = "",
    val jobId: String = "",
    val clientId: String = "",
    val providerId: String? = null,
    val amount: Double = 0.0,
    val platformFee: Double = 0.0,
    val providerPayout: Double = 0.0,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val paymentMethod: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val paidOutAt: Long? = null
)

enum class TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    HELD_IN_ESCROW,
    PAID_OUT
}

// ==================== NOTIFICATION MODELS ====================

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val data: Map<String, String> = emptyMap(),
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    GENERAL,
    JOB_UPDATE,
    PAYMENT,
    MESSAGE,
    RATING,
    SYSTEM
}

// ==================== MESSAGING MODELS ====================

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

data class Conversation(
    val id: String = "",
    val jobId: String = "",
    val clientId: String = "",
    val providerId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

// ==================== RATING/REVIEW MODELS ====================

data class Rating(
    val id: String = "",
    val jobId: String = "",
    val providerId: String = "",
    val clientId: String = "",
    val rating: Int = 0, // 1-5
    val review: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)