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
            Log.d("ListingRepository", "🚀 Starting listing creation")
            Log.d("ListingRepository", "   User ID: ${listing.user_id}")
            Log.d("ListingRepository", "   Breed: ${listing.breed}")
            Log.d("ListingRepository", "   Price: ${listing.price}")
            Log.d("ListingRepository", "   Images to upload: ${imageUris.size}")

            // Generate unique listing ID
            val listingId = firestore.collection("listings").document().id
            Log.d("ListingRepository", "   Generated listing ID: $listingId")

            // Upload images with the listing ID
            val uploadedImageUrls: List<String> = if (imageUris.isNotEmpty()) {
                Log.d("ListingRepository", "📤 Starting image upload...")
                val urls = SupabaseStorageHelper.uploadImagesWithProgress(
                    context,
                    imageUris,
                    listingId
                ) { progress ->
                    onProgress(progress)
                    Log.d("ListingRepository", "   Upload progress: ${(progress * 100).toInt()}%")
                }
                Log.d("ListingRepository", "✅ Images uploaded: ${urls.size} URLs")
                urls
            } else {
                Log.d("ListingRepository", "⚠️ No images to upload")
                emptyList()
            }

            // Create listing with uploaded images
            val listingWithImages = listing.copy(
                id = listingId,
                image_urls = uploadedImageUrls,
                is_active = true
            )

            Log.d("ListingRepository", "💾 Saving to Firestore...")
            Log.d("ListingRepository", "   Document ID: $listingId")
            Log.d("ListingRepository", "   Image URLs: ${uploadedImageUrls.size}")
            Log.d("ListingRepository", "   Tier: ${listingWithImages.listingTier}")
            Log.d("ListingRepository", "   Active: ${listingWithImages.is_active}")

            // Save to Firestore with explicit document ID
            listingsCollection.document(listingId)
                .set(listingWithImages.toMap())
                .await()

            Log.d("ListingRepository", "✅✅✅ LISTING CREATED SUCCESSFULLY IN FIRESTORE! ✅✅✅")
            Log.d("ListingRepository", "   Listing ID: $listingId")
            Log.d("ListingRepository", "   Document path: listings/$listingId")

            // Verify the document was created
            val verifyDoc = listingsCollection.document(listingId).get().await()
            if (verifyDoc.exists()) {
                Log.d("ListingRepository", "✅ VERIFICATION: Document exists in Firestore!")
                Log.d("ListingRepository", "   Data: ${verifyDoc.data}")
            } else {
                Log.e("ListingRepository", "❌ VERIFICATION FAILED: Document not found!")
            }

            true

        } catch (e: Exception) {
            Log.e("ListingRepository", "❌ Error creating listing", e)
            Log.e("ListingRepository", "   Exception type: ${e.javaClass.simpleName}")
            Log.e("ListingRepository", "   Message: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteListing(listingId: String): Boolean {
        return try {
            Log.d("ListingRepository", "🗑️ Deleting listing: $listingId")

            // Get listing first to delete images
            val doc = listingsCollection.document(listingId).get().await()
            val listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)

            // Delete images from Supabase
            listing?.image_urls?.let { urls ->
                if (urls.isNotEmpty()) {
                    Log.d("ListingRepository", "   Deleting ${urls.size} images from storage")
                    SupabaseStorageHelper.deleteImagesByUrls(urls)
                }
            }

            // Delete from Firestore
            listingsCollection.document(listingId).delete().await()
            Log.d("ListingRepository", "✅ Listing deleted successfully")
            true
        } catch (e: Exception) {
            Log.e("ListingRepository", "❌ Error deleting listing", e)
            false
        }
    }

    suspend fun getAllActiveListings(): List<Listing> {
        return try {
            Log.d("ListingRepository", "📋 Fetching all active listings...")

            val snapshot = listingsCollection
                .whereEqualTo("is_active", true)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val listings = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ListingRepository", "❌ Error parsing listing ${doc.id}", e)
                    null
                }
            }

            Log.d("ListingRepository", "✅ Fetched ${listings.size} active listings")
            listings
        } catch (e: Exception) {
            Log.e("ListingRepository", "❌ Error getting active listings", e)
            emptyList()
        }
    }

    suspend fun getListingsByUserId(userId: String): List<Listing> {
        return try {
            Log.d("ListingRepository", "📋 Fetching listings for user: $userId")

            val snapshot = listingsCollection
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val listings = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ListingRepository", "❌ Error parsing listing ${doc.id}", e)
                    null
                }
            }

            Log.d("ListingRepository", "✅ Fetched ${listings.size} listings for user")
            listings
        } catch (e: Exception) {
            Log.e("ListingRepository", "❌ Error getting user listings", e)
            emptyList()
        }
    }

    suspend fun updateListing(listing: Listing): Boolean {
        return try {
            Log.d("ListingRepository", "📝 Updating listing: ${listing.id}")

            listingsCollection.document(listing.id)
                .set(listing.toMap())
                .await()

            Log.d("ListingRepository", "✅ Listing updated successfully")
            true
        } catch (e: Exception) {
            Log.e("ListingRepository", "❌ Error updating listing", e)
            false
        }
    }

    suspend fun toggleListingActive(listingId: String, isActive: Boolean): Boolean {
        return try {
            Log.d("ListingRepository", "🔄 Toggling listing active: $listingId -> $isActive")

            listingsCollection.document(listingId)
                .update("is_active", isActive)
                .await()

            Log.d("ListingRepository", "✅ Listing active status updated")
            true
        } catch (e: Exception) {
            Log.e("ListingRepository", "❌ Error toggling listing active", e)
            false
        }
    }

    suspend fun getListingById(listingId: String): Listing? {
        return try {
            Log.d("ListingRepository", "🔍 Fetching listing by ID: $listingId")

            val doc = listingsCollection.document(listingId).get().await()
            if (doc.exists()) {
                val listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)
                Log.d("ListingRepository", "✅ Listing found: ${listing?.breed}")
                listing
            } else {
                Log.w("ListingRepository", "⚠️ Listing not found: $listingId")
                null
            }
        } catch (e: Exception) {
            Log.e("ListingRepository", "❌ Error getting listing by ID", e)
            null
        }
    }
}