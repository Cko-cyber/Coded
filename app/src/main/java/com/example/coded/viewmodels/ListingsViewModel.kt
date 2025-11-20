package com.example.coded.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ListingsUiState {
    object Loading : ListingsUiState()
    object Empty : ListingsUiState()
    data class Success(val listings: List<Listing>) : ListingsUiState()
    data class Error(val message: String) : ListingsUiState()
}

class ListingsViewModel : ViewModel() {
    private val listingRepository = ListingRepository()

    private val _uiState = MutableStateFlow<ListingsUiState>(ListingsUiState.Loading)
    val uiState: StateFlow<ListingsUiState> = _uiState.asStateFlow()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadListings()
    }

    fun loadListings() {
        viewModelScope.launch {
            try {
                _uiState.value = ListingsUiState.Loading
                _isLoading.value = true
                _error.value = null

                val allListings = listingRepository.getAllActiveListings()
                _listings.value = allListings

                _uiState.value = if (allListings.isEmpty()) {
                    ListingsUiState.Empty
                } else {
                    ListingsUiState.Success(allListings)
                }

                _error.value = null
            } catch (e: Exception) {
                val errorMessage = "Failed to load listings: ${e.message}"
                _error.value = errorMessage
                _uiState.value = ListingsUiState.Error(errorMessage)
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