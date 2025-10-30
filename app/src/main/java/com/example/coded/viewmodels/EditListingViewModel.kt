package com.example.coded.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class EditListingViewModel : ViewModel() {
    val listing = mutableStateOf<com.example.coded.data.Listing?>(null)
    val isLoading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun loadListing(listingId: String) {
        isLoading.value = true
        // TODO: Implement loading logic from Firestore
        // For now, just set loading to false
        isLoading.value = false
    }
}