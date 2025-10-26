package com.example.coded.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import com.example.coded.data.User
import com.example.coded.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SingleStockViewModel : ViewModel() {
    private val listingRepository = ListingRepository()
    private val userRepository = UserRepository()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _seller = MutableStateFlow<User?>(null)
    val seller: StateFlow<User?> = _seller

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // FIXED: Use correct method name
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
}