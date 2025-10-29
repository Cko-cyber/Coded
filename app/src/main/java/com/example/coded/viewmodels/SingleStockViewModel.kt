package com.example.coded.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import com.example.coded.data.User
import com.example.coded.data.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SingleStockViewModel : ViewModel() {
    private val listingRepository = ListingRepository()
    private val userRepository = UserRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _seller = MutableStateFlow<User?>(null)
    val seller: StateFlow<User?> = _seller

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Add shortlist state
    private val _isInShortlist = MutableStateFlow(false)
    val isInShortlist: StateFlow<Boolean> = _isInShortlist

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val listingData = listingRepository.getListingById(listingId)
                _listing.value = listingData

                // Load seller information
                listingData?.let { listing ->
                    val sellerData = userRepository.getUser(listing.user_id)
                    _seller.value = sellerData
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load listing: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Check if listing is in user's shortlist
    suspend fun checkIfInShortlist(userId: String, listingId: String) {
        try {
            val doc = firestore.collection("shortlist")
                .document("${userId}_${listingId}")
                .get()
                .await()

            _isInShortlist.value = doc.exists()
        } catch (e: Exception) {
            // Silently fail - shortlist check is not critical
            _isInShortlist.value = false
        }
    }

    // Add to shortlist
    suspend fun addToShortlist(userId: String, listingId: String) {
        try {
            val shortlistItem = mapOf(
                "userId" to userId,
                "listingId" to listingId,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("shortlist")
                .document("${userId}_${listingId}")
                .set(shortlistItem)
                .await()

            _isInShortlist.value = true
        } catch (e: Exception) {
            _error.value = "Failed to add to shortlist: ${e.message}"
        }
    }

    // Remove from shortlist
    suspend fun removeFromShortlist(userId: String, listingId: String) {
        try {
            firestore.collection("shortlist")
                .document("${userId}_${listingId}")
                .delete()
                .await()

            _isInShortlist.value = false
        } catch (e: Exception) {
            _error.value = "Failed to remove from shortlist: ${e.message}"
        }
    }
}