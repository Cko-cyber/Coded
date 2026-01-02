package com.example.coded.models

import com.google.firebase.Timestamp

/**
 * Provider Trust Score (0-100)
 *
 * Determines:
 * - Job visibility
 * - Job value caps
 * - Dispatch priority
 * - Access to premium clients
 */
enum class TrustTier {
    ELITE,      // 85-100
    TRUSTED,    // 70-84
    RESTRICTED, // 50-69
    PROBATION,  // 30-49
    SUSPENDED;  // <30

    companion object {
        fun fromScore(score: Int): TrustTier {
            return when {
                score >= 85 -> ELITE
                score >= 70 -> TRUSTED
                score >= 50 -> RESTRICTED
                score >= 30 -> PROBATION
                else -> SUSPENDED
            }
        }
    }
}

data class ProviderTrustScore(
    val providerId: String = "",
    val score: Int = 60, // Start at 60
    val tier: TrustTier = TrustTier.RESTRICTED,

    // Performance metrics
    val jobsCompleted: Int = 0,
    val jobsDisputed: Int = 0,
    val noShowCount: Int = 0,
    val lateCompletions: Int = 0,
    val cleanStreak: Int = 0, // Consecutive clean jobs

    // Tier effects
    val maxJobValue: Long = 5000, // Restricted tier cap
    val canAccessPremiumClients: Boolean = false,
    val dispatchPriority: Int = 3, // 1=highest, 5=lowest

    // History
    val scoreHistory: List<ScoreAdjustment> = emptyList(),
    val lastUpdated: Timestamp = Timestamp.now(),

    // Onboarding
    val onboardingType: String = "EXPERIENCE", // "CREDENTIAL" or "EXPERIENCE"
    val verificationStatus: String = "PENDING", // "PENDING", "VERIFIED", "REJECTED"
    val credentialDocuments: List<String> = emptyList()
) {
    /**
     * Calculate tier effects based on score
     */
    fun recalculateTier(): ProviderTrustScore {
        val newTier = TrustTier.fromScore(score)
        val (maxValue, premiumAccess, priority) = when (newTier) {
            TrustTier.ELITE -> Triple(50000L, true, 1)
            TrustTier.TRUSTED -> Triple(20000L, true, 2)
            TrustTier.RESTRICTED -> Triple(5000L, false, 3)
            TrustTier.PROBATION -> Triple(2000L, false, 4)
            TrustTier.SUSPENDED -> Triple(0L, false, 5)
        }

        return this.copy(
            tier = newTier,
            maxJobValue = maxValue,
            canAccessPremiumClients = premiumAccess,
            dispatchPriority = priority
        )
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "providerId" to providerId,
            "score" to score,
            "tier" to tier.name,
            "jobsCompleted" to jobsCompleted,
            "jobsDisputed" to jobsDisputed,
            "noShowCount" to noShowCount,
            "lateCompletions" to lateCompletions,
            "cleanStreak" to cleanStreak,
            "maxJobValue" to maxJobValue,
            "canAccessPremiumClients" to canAccessPremiumClients,
            "dispatchPriority" to dispatchPriority,
            "scoreHistory" to scoreHistory.map { it.toMap() },
            "lastUpdated" to lastUpdated,
            "onboardingType" to onboardingType,
            "verificationStatus" to verificationStatus,
            "credentialDocuments" to credentialDocuments
        )
    }
}

/**
 * Client Risk Score (0-100) - Hidden from client
 */
enum class ClientRiskTier {
    PREFERRED,  // 80-100
    STANDARD,   // 60-79
    RISKY,      // 40-59
    RESTRICTED, // 20-39
    BANNED;     // <20

    companion object {
        fun fromScore(score: Int): ClientRiskTier {
            return when {
                score >= 80 -> PREFERRED
                score >= 60 -> STANDARD
                score >= 40 -> RISKY
                score >= 20 -> RESTRICTED
                else -> BANNED
            }
        }
    }
}

data class ClientRiskScore(
    val clientId: String = "",
    val score: Int = 70, // Start at 70
    val tier: ClientRiskTier = ClientRiskTier.STANDARD,

    // Behavior metrics
    val jobsCreated: Int = 0,
    val falseDisputes: Int = 0,
    val abuseReports: Int = 0,
    val fastVerifications: Int = 0, // Verified within 12 hours

    // Tier effects (hidden from client)
    val requiresPreFunding: Boolean = false,
    val limitedProviderPool: Boolean = false,
    val isBanned: Boolean = false,

    val scoreHistory: List<ScoreAdjustment> = emptyList(),
    val lastUpdated: Timestamp = Timestamp.now()
) {
    fun recalculateTier(): ClientRiskScore {
        val newTier = ClientRiskTier.fromScore(score)
        val (preFund, limited, banned) = when (newTier) {
            ClientRiskTier.PREFERRED -> Triple(false, false, false)
            ClientRiskTier.STANDARD -> Triple(false, false, false)
            ClientRiskTier.RISKY -> Triple(true, false, false)
            ClientRiskTier.RESTRICTED -> Triple(true, true, false)
            ClientRiskTier.BANNED -> Triple(true, true, true)
        }

        return this.copy(
            tier = newTier,
            requiresPreFunding = preFund,
            limitedProviderPool = limited,
            isBanned = banned
        )
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "clientId" to clientId,
            "score" to score,
            "tier" to tier.name,
            "jobsCreated" to jobsCreated,
            "falseDisputes" to falseDisputes,
            "abuseReports" to abuseReports,
            "fastVerifications" to fastVerifications,
            "requiresPreFunding" to requiresPreFunding,
            "limitedProviderPool" to limitedProviderPool,
            "isBanned" to isBanned,
            "scoreHistory" to scoreHistory.map { it.toMap() },
            "lastUpdated" to lastUpdated
        )
    }
}

/**
 * Score adjustment record
 */
data class ScoreAdjustment(
    val amount: Int,
    val reason: String,
    val jobId: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "amount" to amount,
            "reason" to reason,
            "jobId" to jobId,
            "timestamp" to timestamp
        )
    }
}