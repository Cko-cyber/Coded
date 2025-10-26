package com.example.coded.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListingsViewModel : ViewModel() {
    private val listingRepository = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadListings()
    }

    fun loadListings() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val allListings = listingRepository.getAllActiveListings()
                _listings.value = allListings
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load listings: ${e.message}"
                _listings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchListings(query: String): List<Listing> {
        return if (query.isBlank()) {
            _listings.value
        } else {
            _listings.value.filter { listing ->
                listing.breed.contains(query, ignoreCase = true) ||
                        listing.location.contains(query, ignoreCase = true) ||
                        listing.age.contains(query, ignoreCase = true)
            }
        }
    }

    fun refreshListings() {
        loadListings()
    }
}