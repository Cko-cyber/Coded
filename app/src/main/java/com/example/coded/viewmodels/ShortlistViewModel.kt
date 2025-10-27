package com.example.coded.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShortlistViewModel : ViewModel() {
    private val listingRepository = ListingRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _shortlistedListings = MutableStateFlow<List<Listing>>(emptyList())
    val shortlistedListings: StateFlow<List<Listing>> = _shortlistedListings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadShortlist(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Get shortlist IDs
                val shortlistSnapshot = firestore.collection("shortlist")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val listingIds = shortlistSnapshot.documents.mapNotNull {
                    it.getString("listingId")
                }

                if (listingIds.isEmpty()) {
                    _shortlistedListings.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                // Fetch the actual listings
                val listings = mutableListOf<Listing>()
                for (listingId in listingIds) {
                    val listing = listingRepository.getListingById(listingId)
                    if (listing != null && listing.is_active) {
                        listings.add(listing)
                    }
                }

                _shortlistedListings.value = listings
                _error.value = null
                Log.d("ShortlistViewModel", "✅ Loaded ${listings.size} shortlisted items")

            } catch (e: Exception) {
                Log.e("ShortlistViewModel", "Error loading shortlist", e)
                _error.value = "Failed to load shortlist: ${e.message}"
                _shortlistedListings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun addToShortlist(userId: String, listingId: String): Boolean {
        return try {
            val shortlistItem = mapOf(
                "userId" to userId,
                "listingId" to listingId,
                "createdAt" to Timestamp.now()
            )

            firestore.collection("shortlist")
                .document("${userId}_${listingId}")
                .set(shortlistItem)
                .await()

            Log.d("ShortlistViewModel", "✅ Added to shortlist: $listingId")

            // Reload shortlist
            loadShortlist(userId)
            true
        } catch (e: Exception) {
            Log.e("ShortlistViewModel", "Error adding to shortlist", e)
            _error.value = "Failed to add to shortlist: ${e.message}"
            false
        }
    }

    suspend fun removeFromShortlist(userId: String, listingId: String): Boolean {
        return try {
            firestore.collection("shortlist")
                .document("${userId}_${listingId}")
                .delete()
                .await()

            Log.d("ShortlistViewModel", "✅ Removed from shortlist: $listingId")

            // Update local state
            _shortlistedListings.value = _shortlistedListings.value.filter {
                it.id != listingId
            }

            true
        } catch (e: Exception) {
            Log.e("ShortlistViewModel", "Error removing from shortlist", e)
            _error.value = "Failed to remove from shortlist: ${e.message}"
            false
        }
    }

    suspend fun isInShortlist(userId: String, listingId: String): Boolean {
        return try {
            val doc = firestore.collection("shortlist")
                .document("${userId}_${listingId}")
                .get()
                .await()

            doc.exists()
        } catch (e: Exception) {
            Log.e("ShortlistViewModel", "Error checking shortlist", e)
            false
        }
    }
}