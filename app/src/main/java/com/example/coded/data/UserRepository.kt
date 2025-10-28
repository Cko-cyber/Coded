package com.example.coded.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun createUser(user: User): Boolean {
        return try {
            // Use the toMap() method from your User class
            usersCollection.document(user.id).set(user.toMap()).await()
            true
        } catch (e: Exception) {
            println("Error creating user: ${e.message}")
            false
        }
    }

    suspend fun updateUser(user: User): Boolean {
        return try {
            val updateData = mapOf(
                "mobile_number" to user.mobile_number,
                "full_name" to user.full_name,
                "email" to user.email,
                "location" to user.location,
                "profile_pic" to user.profile_pic,
                "token_balance" to user.token_balance,
                "free_listings_used" to user.free_listings_used,
                "updated_at" to Timestamp.now(),
                "last_active" to Timestamp.now()
            )
            usersCollection.document(user.id).update(updateData).await()
            true
        } catch (e: Exception) {
            println("Error updating user: ${e.message}")
            false
        }
    }

    suspend fun getUser(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error getting user: ${e.message}")
            null
        }
    }

    // Add method to update token balance specifically
    suspend fun updateTokenBalance(userId: String, newBalance: Int): Boolean {
        return try {
            usersCollection.document(userId).update(
                mapOf(
                    "token_balance" to newBalance,
                    "updated_at" to Timestamp.now(),
                    "last_active" to Timestamp.now()
                )
            ).await()
            true
        } catch (e: Exception) {
            println("Error updating token balance: ${e.message}")
            false
        }
    }

    // Add method to increment free listings used
    suspend fun incrementFreeListingsUsed(userId: String): Boolean {
        return try {
            val user = getUser(userId)
            if (user != null) {
                val newCount = user.free_listings_used + 1
                usersCollection.document(userId).update(
                    mapOf(
                        "free_listings_used" to newCount,
                        "updated_at" to Timestamp.now(),
                        "last_active" to Timestamp.now()
                    )
                ).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error incrementing free listings: ${e.message}")
            false
        }
    }

    // Alternative method to increment free listings without fetching user first
    suspend fun incrementFreeListingsUsedDirect(userId: String): Boolean {
        return try {
            usersCollection.document(userId).update(
                "free_listings_used", com.google.firebase.firestore.FieldValue.increment(1),
                "updated_at", Timestamp.now(),
                "last_active", Timestamp.now()
            ).await()
            true
        } catch (e: Exception) {
            println("Error incrementing free listings directly: ${e.message}")
            false
        }
    }

    // Method to decrement token balance (alternative approach)
    suspend fun decrementTokenBalance(userId: String): Boolean {
        return try {
            usersCollection.document(userId).update(
                "token_balance", com.google.firebase.firestore.FieldValue.increment(-1),
                "updated_at", Timestamp.now(),
                "last_active", Timestamp.now()
            ).await()
            true
        } catch (e: Exception) {
            println("Error decrementing token balance: ${e.message}")
            false
        }
    }

    // Method to update last_active timestamp
    suspend fun updateLastActive(userId: String): Boolean {
        return try {
            usersCollection.document(userId).update(
                "last_active", Timestamp.now()
            ).await()
            true
        } catch (e: Exception) {
            println("Error updating last active: ${e.message}")
            false
        }
    }
}