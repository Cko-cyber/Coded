package com.example.coded.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ListingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val listingsCollection = firestore.collection("listings")

    suspend fun createListing(
        context: Context,
        listing: Listing,
        imageUris: List<Uri>,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        return try {
            // Upload images
            val uploadedImageUrls: List<String> = if (imageUris.isNotEmpty()) {
                SupabaseStorageHelper.uploadImagesWithProgress(
                    context,
                    imageUris,
                    listing.id
                ) { progress -> onProgress(progress) }
            } else {
                emptyList()
            }

            Log.d("ListingRepository", "Uploaded ${uploadedImageUrls.size} images")

            // Create listing with images
            val listingWithImages = listing.copy(
                image_urls = uploadedImageUrls,
                is_active = true
            )

            // Save to Firestore
            listingsCollection.document(listing.id)
                .set(listingWithImages.toMap())
                .await()

            Log.d("ListingRepository", "Listing created successfully: ${listing.id}")
            true

        } catch (e: Exception) {
            Log.e("ListingRepository", "Error creating listing", e)
            false
        }
    }

    suspend fun deleteListing(listingId: String): Boolean {
        return try {
            // Get listing first to delete images
            val doc = listingsCollection.document(listingId).get().await()
            val listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)

            // Delete images from Supabase
            listing?.image_urls?.let { urls ->
                if (urls.isNotEmpty()) {
                    SupabaseStorageHelper.deleteImagesByUrls(urls)
                }
            }

            // Delete from Firestore
            listingsCollection.document(listingId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("ListingRepository", "Error deleting listing", e)
            false
        }
    }

    suspend fun getAllActiveListings(): List<Listing> {
        return try {
            val snapshot = listingsCollection
                .whereEqualTo("is_active", true)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ListingRepository", "Error parsing listing ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ListingRepository", "Error getting active listings", e)
            emptyList()
        }
    }

    suspend fun getListingsByUserId(userId: String): List<Listing> {
        return try {
            val snapshot = listingsCollection
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ListingRepository", "Error parsing listing ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ListingRepository", "Error getting user listings", e)
            emptyList()
        }
    }

    suspend fun updateListing(listing: Listing): Boolean {
        return try {
            listingsCollection.document(listing.id)
                .set(listing.toMap())
                .await()
            true
        } catch (e: Exception) {
            Log.e("ListingRepository", "Error updating listing", e)
            false
        }
    }

    suspend fun toggleListingActive(listingId: String, isActive: Boolean): Boolean {
        return try {
            listingsCollection.document(listingId)
                .update("is_active", isActive)
                .await()
            true
        } catch (e: Exception) {
            Log.e("ListingRepository", "Error toggling listing active", e)
            false
        }
    }

    suspend fun getListingById(listingId: String): Listing? {
        return try {
            val doc = listingsCollection.document(listingId).get().await()
            if (doc.exists()) {
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            } else null
        } catch (e: Exception) {
            Log.e("ListingRepository", "Error getting listing by ID", e)
            null
        }
    }
}
// REMOVE THE DUPLICATE METHOD BELOW - Your file should end here