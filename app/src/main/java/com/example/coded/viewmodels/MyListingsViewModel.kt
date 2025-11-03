package com.example.coded.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyListingsViewModel : ViewModel() {
    private val TAG = "MyListingsVM"
    private val firestore = FirebaseFirestore.getInstance()

    private var listenerRegistration: ListenerRegistration? = null
    private var currentUserId: String? = null

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _operationInProgress = MutableStateFlow<String?>(null)
    val operationInProgress: StateFlow<String?> = _operationInProgress

    init {
        Log.d(TAG, "✅ MyListingsViewModel initialized")
    }

    // Real-time listener for user's listings
    fun loadUserListings(userId: String) {
        Log.d(TAG, "📥 Setting up real-time listener for user: $userId")
        currentUserId = userId
        _isLoading.value = true
        _error.value = null

        // Remove any existing listener
        listenerRegistration?.remove()

        // Set up new real-time listener
        listenerRegistration = firestore.collection("listings")
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    Log.e(TAG, "❌ Listener error: ${error.message}")
                    _error.value = "Failed to load listings: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val userListings = snapshot.documents.mapNotNull { doc ->
                        try {
                            val listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)
                            Log.d(TAG, "📄 Loaded listing: ${listing?.breed} - Active: ${listing?.is_active} - ID: ${listing?.id}")
                            listing
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing listing ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    Log.d(TAG, "✅ Real-time update: ${userListings.size} listings")
                    _listings.value = userListings
                    _error.value = null
                } else {
                    Log.d(TAG, "📭 No listings found for user: $userId")
                    _listings.value = emptyList()
                    _error.value = null
                }
            }
    }

    fun toggleListingActive(listingId: String, isActive: Boolean) {
        _operationInProgress.value = "Updating listing..."

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Toggling listing $listingId to active=$isActive")

                // FIXED: Use individual field updates with correct field names
                firestore.collection("listings")
                    .document(listingId)
                    .update(
                        "is_active", isActive,
                        "updated_at", Timestamp.now()
                    )
                    .await()

                Log.d(TAG, "✅ Listing toggled successfully in Firestore")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error toggling listing: ${e.message}", e)
                _error.value = "Failed to update listing: ${e.message}"
            } finally {
                _operationInProgress.value = null
            }
        }
    }

    fun deleteListing(listingId: String) {
        _operationInProgress.value = "Deleting listing..."

        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Deleting listing: $listingId")

                // Delete from Firestore
                firestore.collection("listings")
                    .document(listingId)
                    .delete()
                    .await()

                Log.d(TAG, "✅ Deleted listing: $listingId")

                // Remove from local state immediately for better UX
                _listings.value = _listings.value.filter { it.id != listingId }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting listing: ${e.message}", e)
                _error.value = "Failed to delete listing: ${e.message}"
            } finally {
                _operationInProgress.value = null
            }
        }
    }

    fun refreshUserListings() {
        currentUserId?.let { userId ->
            Log.d(TAG, "🔄 Manual refresh requested for user: $userId")
            loadUserListings(userId)
        } ?: run {
            Log.e(TAG, "❌ Cannot refresh: currentUserId is null")
            _error.value = "Cannot refresh: User not set"
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Stop the listener when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        Log.d(TAG, "🧹 ViewModel cleared, listener removed")
    }
}