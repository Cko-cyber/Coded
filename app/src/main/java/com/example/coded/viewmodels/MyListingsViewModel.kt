package com.example.coded.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyListingsViewModel : ViewModel() {
    private val listingRepository = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUserListings(userId: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val userListings = listingRepository.getListingsByUserId(userId)
                _listings.value = userListings
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load your listings: ${e.message}"
                _listings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun toggleListingActive(listingId: String, isActive: Boolean): Boolean {
        return try {
            val success = listingRepository.toggleListingActive(listingId, isActive)
            if (success) {
                // Update local state
                _listings.value = _listings.value.map { listing ->
                    if (listing.id == listingId) {
                        listing.copy(is_active = isActive)
                    } else {
                        listing
                    }
                }
            }
            success
        } catch (e: Exception) {
            _error.value = "Failed to update listing: ${e.message}"
            false
        }
    }

    suspend fun deleteListing(listingId: String): Boolean {
        return try {
            val success = listingRepository.deleteListing(listingId)
            if (success) {
                // Remove from local state
                _listings.value = _listings.value.filter { it.id != listingId }
            }
            success
        } catch (e: Exception) {
            _error.value = "Failed to delete listing: ${e.message}"
            false
        }
    }

    fun refreshUserListings(userId: String) {
        loadUserListings(userId)
    }
}