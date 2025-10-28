package com.example.coded.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class TokenService {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "TokenService"

    suspend fun canUseFreeListingThisMonth(userId: String): Boolean {
        return try {
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val listingsRef = firestore.collection("listings")
            val snapshot = listingsRef
                .whereEqualTo("user_id", userId)
                .whereEqualTo("listingTier", "FREE")
                .whereGreaterThanOrEqualTo("created_at", Timestamp(monthStart.time))
                .get()
                .await()

            val freeListingsThisMonth = snapshot.size()
            Log.d(TAG, "📊 Free listings this month: $freeListingsThisMonth")

            // Allow max 3 free listings per month
            freeListingsThisMonth < 3

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking free listing usage: ${e.message}")
            false
        }
    }

    suspend fun hasEnoughTokens(userId: String, tier: ListingTier): Boolean {
        val userRef = firestore.collection("users").document(userId).get().await()
        val tokens = userRef.getLong("token_balance") ?: 0L  // Changed from "tokens" to "token_balance"
        val cost = getTokenCost(tier).toLong()  // Convert to Long for comparison
        return tokens >= cost
    }

    suspend fun deductTokens(userId: String, tier: ListingTier): Result<Long> {
        return try {
            val userRef = firestore.collection("users").document(userId)  // Changed from "profiles" to "users"
            val snapshot = userRef.get().await()
            val tokens = snapshot.getLong("token_balance") ?: 0L  // Changed from "tokens" to "token_balance"
            val cost = getTokenCost(tier).toLong()  // Convert to Long
            if (tokens < cost) return Result.failure(Exception("Not enough tokens"))

            val newBalance = tokens - cost
            userRef.update("token_balance", newBalance).await()
            Result.success(newBalance)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Token deduction failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun getTokenCost(tier: ListingTier): Int {
        return when (tier) {
            ListingTier.FREE -> 0
            ListingTier.BASIC-> 1
            ListingTier.BULK-> 3
            ListingTier.PREMIUM  -> 5
            else -> 0
        }
    }
}