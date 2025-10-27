package com.example.coded.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TokenService {
    private val firestore = FirebaseFirestore.getInstance()

    // Supabase configuration
    private val SUPABASE_PROJECT_URL = "https://vxetgoaowehxxifdbdmm.supabase.co"
    private val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4ZXRnb2Fvd2VoeHhpZmRiZG1tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MjE4MzYsImV4cCI6MjA3NjQ5NzgzNn0.n7V7iNzNkMjzb2aNq_Z2ANoC7EhmdQcFB4H3tRBnNVM"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Get token cost for a listing tier
     */
    fun getTokenCost(tier: ListingTier): Int {
        return when (tier) {
            ListingTier.FREE -> 0
            ListingTier.BASIC -> 5
            ListingTier.BULK -> 10
            ListingTier.PREMIUM -> 20
        }
    }

    /**
     * Check if user has enough tokens
     */
    suspend fun hasEnoughTokens(userId: String, tier: ListingTier): Boolean {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val tokenBalance = doc.getLong("token_balance")?.toInt() ?: 0
            val cost = getTokenCost(tier)

            tokenBalance >= cost
        } catch (e: Exception) {
            Log.e("TokenService", "Error checking token balance", e)
            false
        }
    }

    /**
     * Check if user can use free listing
     */
    suspend fun canUseFreeListingthis Month(userId: String): Boolean {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val freeListingsUsed = doc.getLong("free_listings_used")?.toInt() ?: 0
            val resetDate = doc.getTimestamp("free_listings_reset_date")

            // Check if we need to reset the count (new month)
            val now = System.currentTimeMillis()
            val lastReset = resetDate?.toDate()?.time ?: 0
            val daysSinceReset = (now - lastReset) / (1000 * 60 * 60 * 24)

            if (daysSinceReset >= 30) {
                // Reset the counter
                firestore.collection("users").document(userId).update(
                    mapOf(
                        "free_listings_used" to 0,
                        "free_listings_reset_date" to Timestamp.now()
                    )
                ).await()
                return true
            }

            freeListingsUsed < 3
        } catch (e: Exception) {
            Log.e("TokenService", "Error checking free listings", e)
            false
        }
    }

    /**
     * Deduct tokens for a listing using Supabase Edge Function
     */
    suspend fun deductTokens(userId: String, tier: ListingTier): Result<Int> {
        return try {
            val cost = getTokenCost(tier)

            // If it's a free listing, just increment the counter
            if (tier == ListingTier.FREE) {
                val canUse = canUseFreeListingthis Month(userId)
                if (!canUse) {
                    return Result.failure(Exception("You've used all 3 free listings this month"))
                }

                firestore.collection("users").document(userId).update(
                    "free_listings_used", FieldValue.increment(1)
                ).await()

                return Result.success(0)
            }

            // For paid tiers, check balance first
            if (!hasEnoughTokens(userId, tier)) {
                return Result.failure(Exception("Insufficient tokens. Need $cost tokens."))
            }

            // Call Supabase Edge Function to deduct tokens
            val edgeFunctionUrl = "$SUPABASE_PROJECT_URL/functions/v1/deduct-tokens"

            val jsonBody = JSONObject().apply {
                put("userId", userId)
                put("amount", cost)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(edgeFunctionUrl)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e("TokenService", "Edge function error: ${response.code}")
                // Fallback: deduct directly in Firestore
                return deductTokensFirestore(userId, cost)
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val newBalance = jsonResponse.optInt("newBalance", -1)

            if (newBalance >= 0) {
                Log.d("TokenService", "✅ Tokens deducted. New balance: $newBalance")
                Result.success(newBalance)
            } else {
                // Fallback to Firestore
                deductTokensFirestore(userId, cost)
            }

        } catch (e: Exception) {
            Log.e("TokenService", "Error deducting tokens", e)
            // Fallback to Firestore
            deductTokensFirestore(userId, getTokenCost(tier))
        }
    }

    /**
     * Fallback: Deduct tokens directly in Firestore
     */
    private suspend fun deductTokensFirestore(userId: String, amount: Int): Result<Int> {
        return try {
            val userRef = firestore.collection("users").document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getLong("token_balance")?.toInt() ?: 0

                if (currentBalance < amount) {
                    throw Exception("Insufficient tokens")
                }

                val newBalance = currentBalance - amount
                transaction.update(userRef, mapOf(
                    "token_balance" to newBalance,
                    "updated_at" to Timestamp.now()
                ))

                newBalance
            }.await().let { newBalance ->
                Log.d("TokenService", "✅ Tokens deducted (Firestore). New balance: $newBalance")
                Result.success(newBalance)
            }
        } catch (e: Exception) {
            Log.e("TokenService", "Firestore deduction failed", e)
            Result.failure(e)
        }
    }

    /**
     * Add tokens to user account (for purchases)
     */
    suspend fun addTokens(userId: String, amount: Int): Result<Int> {
        return try {
            val userRef = firestore.collection("users").document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getLong("token_balance")?.toInt() ?: 0
                val newBalance = currentBalance + amount

                transaction.update(userRef, mapOf(
                    "token_balance" to newBalance,
                    "updated_at" to Timestamp.now()
                ))

                newBalance
            }.await().let { newBalance ->
                Log.d("TokenService", "✅ Tokens added. New balance: $newBalance")
                Result.success(newBalance)
            }
        } catch (e: Exception) {
            Log.e("TokenService", "Error adding tokens", e)
            Result.failure(e)
        }
    }
}