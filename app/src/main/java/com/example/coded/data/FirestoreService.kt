// File: app/src/main/java/com/example/coded/data/FirestoreService.kt
package com.example.coded.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class FirestoreUser(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val location: String = "",
    val organization: String = "",
    val profilePicUrl: String = "",
    val tokenBalance: Int = 0,
    val freeListingsUsed: Int = 0,
    val freeListingsResetDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class FirestoreListing(
    val listingId: String = "",
    val userId: String = "",
    val breed: String = "",
    val age: String = "",
    val price: Double = 0.0,
    val location: String = "",
    val imageUrls: List<String> = emptyList(), // Supabase Storage URLs
    val videoUrl: String = "",                   // Supabase Storage URL
    val vaccinationStatus: String = "",
    val deworming: String = "",
    val fullDetails: String = "",
    val tier: String = "free", // free, basic, bulk, premium
    val tierPrice: Double = 0.0,
    val isActive: Boolean = true,
    val isSold: Boolean = false,
    val viewsCount: Int = 0,
    val expiresAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class FirestoreMessage(
    val messageId: String = "",
    val chatId: String = "",
    val listingId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val mediaUrl: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class FirestoreService {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ==================== USER OPERATIONS ====================

    suspend fun createUser(
        userId: String,
        email: String ,
        firstName: String,
        lastName: String,
        phone: String,
        location: String,
        organization: String
    ): Result<Unit> {
        return try {
            val user = FirestoreUser(
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                location = location,
                organization = organization,
                tokenBalance = 0,
                freeListingsUsed = 0
            )

            db.collection("users")
                .document(userId)
                .set(user)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<FirestoreUser?> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(FirestoreUser::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTokenBalance(userId: String, newBalance: Int): Result<Unit> {
        return updateUser(userId, mapOf("tokenBalance" to newBalance))
    }

    suspend fun incrementFreeListings(userId: String): Result<Unit> {
        return try {
            val user = getUser(userId).getOrNull() ?: return Result.failure(Exception("User not found"))

            db.collection("users")
                .document(userId)
                .update(mapOf("freeListingsUsed" to user.freeListingsUsed + 1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== LISTING OPERATIONS ====================

    suspend fun createListing(listing: FirestoreListing): Result<String> {
        return try {
            val docRef = db.collection("listings")
                .add(listing)
                .await()

            // Update document with its own ID
            docRef.update("listingId", docRef.id).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListing(listingId: String): Result<FirestoreListing?> {
        return try {
            val snapshot = db.collection("listings")
                .document(listingId)
                .get()
                .await()

            val listing = snapshot.toObject(FirestoreListing::class.java)
            Result.success(listing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllListings(): Result<List<FirestoreListing>> {
        return try {
            val snapshot = db.collection("listings")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val listings = snapshot.toObjects(FirestoreListing::class.java)
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserListings(userId: String): Result<List<FirestoreListing>> {
        return try {
            val snapshot = db.collection("listings")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val listings = snapshot.toObjects(FirestoreListing::class.java)
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateListing(listingId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("listings")
                .document(listingId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        return try {
            db.collection("listings")
                .document(listingId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markListingAsSold(listingId: String): Result<Unit> {
        return updateListing(listingId, mapOf("isSold" to true, "isActive" to false))
    }

    suspend fun incrementViewCount(listingId: String): Result<Unit> {
        return try {
            val listing = getListing(listingId).getOrNull() ?: return Result.failure(Exception("Listing not found"))

            db.collection("listings")
                .document(listingId)
                .update("viewsCount", listing.viewsCount + 1)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== MESSAGE OPERATIONS ====================

    suspend fun sendMessage(message: FirestoreMessage): Result<String> {
        return try {
            val docRef = db.collection("messages")
                .add(message)
                .await()

            docRef.update("messageId", docRef.id).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatMessages(chatId: String): Result<List<FirestoreMessage>> {
        return try {
            val snapshot = db.collection("messages")
                .whereEqualTo("chatId", chatId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val messages = snapshot.toObjects(FirestoreMessage::class.java)
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserChats(userId: String): Result<List<String>> {
        return try {
            // Get all unique chat IDs where user is sender or receiver
            val sentSnapshot = db.collection("messages")
                .whereEqualTo("senderId", userId)
                .get()
                .await()

            val receivedSnapshot = db.collection("messages")
                .whereEqualTo("receiverId", userId)
                .get()
                .await()

            val chatIds = mutableSetOf<String>()
            sentSnapshot.documents.forEach { doc ->
                doc.getString("chatId")?.let { chatIds.add(it) }
            }
            receivedSnapshot.documents.forEach { doc ->
                doc.getString("chatId")?.let { chatIds.add(it) }
            }

            Result.success(chatIds.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            db.collection("messages")
                .document(messageId)
                .update("isRead", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== SHORTLIST OPERATIONS ====================

    suspend fun addToShortlist(userId: String, listingId: String): Result<Unit> {
        return try {
            val shortlistItem = mapOf(
                "userId" to userId,
                "listingId" to listingId,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("shortlist")
                .document("${userId}_${listingId}")
                .set(shortlistItem)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromShortlist(userId: String, listingId: String): Result<Unit> {
        return try {
            db.collection("shortlist")
                .document("${userId}_${listingId}")
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShortlistedListings(userId: String): Result<List<FirestoreListing>> {
        return try {
            // Get all shortlist items for user
            val shortlistSnapshot = db.collection("shortlist")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val listingIds = shortlistSnapshot.documents.mapNotNull {
                it.getString("listingId")
            }

            // Get all listings
            val listings = mutableListOf<FirestoreListing>()
            for (listingId in listingIds) {
                val listing = getListing(listingId).getOrNull()
                if (listing != null) {
                    listings.add(listing)
                }
            }

            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}