package com.example.coded.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditListingViewModel : ViewModel() {
    private val TAG = "EditListingVM"
    private val firestore = FirebaseFirestore.getInstance()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadListing(listingId: String) {
        _isLoading.value = true
        _error.value = null
        _listing.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "📥 Loading listing: $listingId")

                val document = firestore.collection("listings")
                    .document(listingId)
                    .get()
                    .await()

                if (document.exists()) {
                    val listingData = document.toObject(Listing::class.java)?.copy(id = document.id)
                    if (listingData != null) {
                        _listing.value = listingData
                        Log.d(TAG, "✅ Listing loaded: ${listingData.breed}")
                    } else {
                        _error.value = "Failed to parse listing data"
                        Log.e(TAG, "❌ Failed to parse listing data")
                    }
                } else {
                    _error.value = "Listing not found"
                    Log.e(TAG, "❌ Listing not found: $listingId")
                }
            } catch (e: Exception) {
                _error.value = "Failed to load listing: ${e.message}"
                Log.e(TAG, "❌ Error loading listing: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateListing(updatedListing: Listing) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "💾 Updating listing: ${updatedListing.id}")

                val listingId = updatedListing.id ?: throw Exception("Listing ID is null")

                // FIXED: Using correct field names from your Listing class
                firestore.collection("listings")
                    .document(listingId)
                    .update(
                        "breed", updatedListing.breed,
                        "age", updatedListing.age,
                        "price", updatedListing.price,
                        "location", updatedListing.location,
                        "deworming", updatedListing.deworming,
                        "vaccination_status", updatedListing.vaccination_status,
                        "full_details", updatedListing.full_details,
                        "updated_at", Timestamp.now()
                    )
                    .await()

                Log.d(TAG, "✅ Listing updated successfully")
                _listing.value = updatedListing

            } catch (e: Exception) {
                _error.value = "Failed to update listing: ${e.message}"
                Log.e(TAG, "❌ Error updating listing: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refreshListing(listingId: String) {
        loadListing(listingId)
    }
}