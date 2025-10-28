package com.example.coded.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import com.example.coded.data.ListingTier
import com.example.coded.data.User
import com.example.coded.data.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SingleStockViewModel : ViewModel() {
    private val listingRepository = ListingRepository()
    private val userRepository = UserRepository()

    // Use mutableStateOf for Compose state
    private val _listing = mutableStateOf<Listing?>(null)
    val listing: State<Listing?> get() = _listing

    private val _seller = mutableStateOf<User?>(null)
    val seller: State<User?> get() = _seller

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> get() = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> get() = _error

    fun loadListing(listingId: String) {
        if (listingId.isBlank()) {
            _error.value = "Invalid listing ID"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _isLoading.value = true
                    _error.value = null
                }

                // Use getListingById instead of getListing
                val listing = listingRepository.getListingById(listingId)

                if (listing == null) {
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        _error.value = "Listing not found"
                    }
                    return@launch
                }

                // Get seller information - make sure user_id exists
                val seller = if (listing.user_id.isNotEmpty()) {
                    userRepository.getUser(listing.user_id)
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    _listing.value = listing
                    _seller.value = seller
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _error.value = "Failed to load listing: ${e.message}"
                }
            }
        }
    }

    // Helper function to get tier from listing
    fun getTierFromListing(listing: Listing): ListingTier {
        return when (listing.listingTier.uppercase()) {
            "FREE" -> ListingTier.FREE
            "BASIC" -> ListingTier.BASIC
            "BULK" -> ListingTier.BULK
            "PREMIUM" -> ListingTier.PREMIUM
            else -> ListingTier.FREE
        }
    }

    // Refresh listing data
    fun refreshListing() {
        val currentListingId = _listing.value?.id
        if (!currentListingId.isNullOrEmpty()) {
            loadListing(currentListingId)
        }
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }
}