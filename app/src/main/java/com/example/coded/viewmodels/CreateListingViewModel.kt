package com.example.coded.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CreateListingUiState(
    val breed: String = "",
    val age: String = "",
    val price: String = "",
    val location: String = "",
    val deworming: String = "",
    val vaccinated: String = "",
    val fullDetails: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val tier: ListingTier = ListingTier.FREE,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val uploadProgress: Map<Uri, Float> = emptyMap()
)

class CreateListingViewModel : ViewModel() {
    private val TAG = "CreateListingVM"
    private val listingRepository = ListingRepository()
    private val tokenService = TokenService()

    private val _uiState = MutableStateFlow(CreateListingUiState())
    val uiState: StateFlow<CreateListingUiState> = _uiState

    // Update form fields
    fun updateBreed(value: String) {
        _uiState.value = _uiState.value.copy(breed = value)
    }

    fun updateAge(value: String) {
        _uiState.value = _uiState.value.copy(age = value)
    }

    fun updatePrice(value: String) {
        _uiState.value = _uiState.value.copy(price = value)
    }

    fun updateLocation(value: String) {
        _uiState.value = _uiState.value.copy(location = value)
    }

    fun updateDeworming(value: String) {
        _uiState.value = _uiState.value.copy(deworming = value)
    }

    fun updateVaccinated(value: String) {
        _uiState.value = _uiState.value.copy(vaccinated = value)
    }

    fun updateFullDetails(value: String) {
        _uiState.value = _uiState.value.copy(fullDetails = value)
    }

    fun updateTier(tier: ListingTier) {
        _uiState.value = _uiState.value.copy(tier = tier)

        val currentImages = _uiState.value.selectedImages
        if (currentImages.size > tier.maxImages) {
            val updatedImages = currentImages.take(tier.maxImages)
            _uiState.value = _uiState.value.copy(selectedImages = updatedImages)
            Log.d(TAG, "📸 Tier changed to ${tier.displayName}. Images reduced to ${updatedImages.size}/${tier.maxImages}")
        }
    }

    fun addImages(uris: List<Uri>) {
        val currentImages = _uiState.value.selectedImages
        val maxImages = _uiState.value.tier.maxImages
        val availableSlots = maxImages - currentImages.size

        if (availableSlots > 0) {
            val newImages = (currentImages + uris).take(maxImages)
            _uiState.value = _uiState.value.copy(selectedImages = newImages)
            Log.d(TAG, "📸 Added images. Total: ${newImages.size}/$maxImages")
        } else {
            Log.w(TAG, "⚠️ Cannot add more images. Already at max: $maxImages")
        }
    }

    fun removeImage(uri: Uri) {
        val updated = _uiState.value.selectedImages.filterNot { it == uri }
        _uiState.value = _uiState.value.copy(selectedImages = updated)
        Log.d(TAG, "🗑️ Removed image. Remaining: ${updated.size}")
    }

    fun clearImages() {
        _uiState.value = _uiState.value.copy(selectedImages = emptyList())
        Log.d(TAG, "🗑️ All images cleared")
    }

    fun resetForm() {
        _uiState.value = CreateListingUiState()
        Log.d(TAG, "🔄 Form reset")
    }

    fun createListing(context: Context, userId: String) {
        Log.d(TAG, "🚀 Starting listing creation for user: $userId")

        val state = _uiState.value

        // Validation
        when {
            userId.isBlank() -> {
                Log.e(TAG, "❌ User ID is blank!")
                _uiState.value = state.copy(error = "User not logged in")
                return
            }
            state.breed.isBlank() -> {
                _uiState.value = state.copy(error = "Breed is required")
                return
            }
            state.age.isBlank() -> {
                _uiState.value = state.copy(error = "Age is required")
                return
            }
            state.price.isBlank() -> {
                _uiState.value = state.copy(error = "Price is required")
                return
            }
            state.location.isBlank() -> {
                _uiState.value = state.copy(error = "Location is required")
                return
            }
            state.selectedImages.isEmpty() -> {
                _uiState.value = state.copy(error = "At least one image is required")
                return
            }
        }

        val priceAsLong = state.price.toLongOrNull()
        if (priceAsLong == null) {
            _uiState.value = state.copy(error = "Valid price is required (numbers only)")
            return
        }

        Log.d(TAG, "✅ Validation passed")
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check and deduct tokens BEFORE creating listing
                Log.d(TAG, "💰 Checking tokens for tier: ${state.tier.displayName}")

                if (state.tier == ListingTier.FREE) {
                    val canUse = tokenService.canUseFreeListingthis Month(userId)
                    if (!canUse) {
                        Log.w(TAG, "⚠️ User has used all free listings")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "You've used all 3 free listings this month. Please buy tokens or wait until next month."
                        )
                        return@launch
                    }
                } else {
                    val hasTokens = tokenService.hasEnoughTokens(userId, state.tier)
                    if (!hasTokens) {
                        val cost = tokenService.getTokenCost(state.tier)
                        Log.w(TAG, "⚠️ Insufficient tokens. Need: $cost")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Insufficient tokens. You need ${cost} tokens for ${state.tier.displayName} listing."
                        )
                        return@launch
                    }
                }

                // Upload images
                Log.d(TAG, "📤 Uploading ${state.selectedImages.size} images...")
                val uploadedUrls = SupabaseStorageHelper.uploadImagesWithProgress(
                    context,
                    state.selectedImages,
                    listingId = java.util.UUID.randomUUID().toString()
                ) { overallProgress ->
                    val progressMap = state.selectedImages.associateWith { overallProgress }
                    _uiState.value = _uiState.value.copy(uploadProgress = progressMap)
                }

                if (uploadedUrls.isEmpty()) {
                    Log.e(TAG, "❌ Image upload failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Image upload failed. Please try again."
                    )
                    return@launch
                }

                Log.d(TAG, "✅ Images uploaded successfully: ${uploadedUrls.size} URLs")

                // Create listing object
                val listing = Listing(
                    id = "",
                    user_id = userId,
                    breed = state.breed,
                    age = state.age,
                    price = priceAsLong,
                    location = state.location,
                    full_details = state.fullDetails,
                    deworming = state.deworming,
                    vaccination_status = state.vaccinated,
                    image_urls = uploadedUrls,
                    is_active = true,
                    created_at = Timestamp.now(),
                    listingTier = state.tier.name
                )

                Log.d(TAG, "💾 Creating listing in Firestore...")
                val success = listingRepository.createListing(context, listing, state.selectedImages)

                if (success) {
                    // DEDUCT TOKENS AFTER SUCCESSFUL LISTING CREATION
                    Log.d(TAG, "💰 Deducting tokens for tier: ${state.tier.displayName}")
                    val deductResult = tokenService.deductTokens(userId, state.tier)

                    if (deductResult.isSuccess) {
                        val newBalance = deductResult.getOrNull() ?: 0
                        Log.d(TAG, "✅ Tokens deducted. New balance: $newBalance")
                    } else {
                        Log.e(TAG, "⚠️ Token deduction failed: ${deductResult.exceptionOrNull()?.message}")
                        // Listing was created but token deduction failed
                        // You might want to handle this case differently
                    }

                    Log.d(TAG, "✅✅✅ LISTING CREATED SUCCESSFULLY! ✅✅✅")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                } else {
                    Log.e(TAG, "❌ Listing creation failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create listing. Please try again."
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during listing creation: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
}