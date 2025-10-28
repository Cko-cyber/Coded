package com.example.coded.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyListingsViewModel : ViewModel() {
    private val TAG = "MyListingsVM"
    private val listingRepository = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ✅ Auto-refresh every 10 seconds when viewing screen
    private var isAutoRefreshActive = false

    init {
        Log.d(TAG, "✅ MyListingsViewModel initialized")
    }

    fun startAutoRefresh(userId: String) {
        if (isAutoRefreshActive) return
        isAutoRefreshActive = true

        viewModelScope.launch {
            while (isAutoRefreshActive) {
                Log.d(TAG, "🔄 Auto-refreshing listings...")
                loadUserListings(userId)
                delay(10000) // Refresh every 10 seconds
            }
        }
    }

    fun stopAutoRefresh() {
        isAutoRefreshActive = false
        Log.d(TAG, "⏸️ Auto-refresh stopped")
    }

    fun loadUserListings(userId: String) {
        Log.d(TAG, "📥 Loading listings for user: $userId")
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val userListings = listingRepository.getListingsByUserId(userId)
                Log.d(TAG, "✅ Loaded ${userListings.size} listings")

                // Debug log each listing
                userListings.forEachIndexed { index, listing ->
                    Log.d(TAG, "   [$index] ${listing.breed} - ${listing.listingTier} - Active: ${listing.is_active}")
                }

                _listings.value = userListings
                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading listings: ${e.message}", e)
                _error.value = "Failed to load your listings: ${e.message}"
                _listings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun toggleListingActive(listingId: String, isActive: Boolean): Boolean {
        Log.d(TAG, "🔄 Toggling listing $listingId to active=$isActive")
        return try {
            val success = listingRepository.toggleListingActive(listingId, isActive)
            if (success) {
                // ✅ Immediately update local state
                _listings.value = _listings.value.map { listing ->
                    if (listing.id == listingId) {
                        Log.d(TAG, "✅ Updated listing ${listing.breed} to active=$isActive")
                        listing.copy(is_active = isActive)
                    } else {
                        listing
                    }
                }
                Log.d(TAG, "✅ Listing toggled successfully")
            } else {
                Log.e(TAG, "❌ Repository returned false")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error toggling listing: ${e.message}", e)
            _error.value = "Failed to update listing: ${e.message}"
            false
        }
    }

    suspend fun deleteListing(listingId: String): Boolean {
        Log.d(TAG, "🗑️ Deleting listing: $listingId")
        return try {
            val success = listingRepository.deleteListing(listingId)
            if (success) {
                // ✅ Immediately remove from local state
                val deletedListing = _listings.value.find { it.id == listingId }
                _listings.value = _listings.value.filter { it.id != listingId }
                Log.d(TAG, "✅ Deleted listing: ${deletedListing?.breed}")
            } else {
                Log.e(TAG, "❌ Repository returned false")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting listing: ${e.message}", e)
            _error.value = "Failed to delete listing: ${e.message}"
            false
        }
    }

    fun refreshUserListings(userId: String) {
        Log.d(TAG, "🔄 Manual refresh requested")
        loadUserListings(userId)
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
        Log.d(TAG, "🧹 ViewModel cleared")
    }
}