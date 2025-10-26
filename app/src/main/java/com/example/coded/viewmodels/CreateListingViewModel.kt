package com.example.coded.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.Listing
import com.example.coded.data.ListingRepository
import com.example.coded.data.ListingTier // ADD THIS IMPORT
import com.example.coded.data.SupabaseStorageHelper
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
    val tier: ListingTier = ListingTier.FREE, // NOW WORKS WITH IMPORT
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val uploadProgress: Map<Uri, Float> = emptyMap()
)

class CreateListingViewModel : ViewModel() {
    private val TAG = "CreateListingVM"
    private val listingRepository = ListingRepository()

    private val _uiState = MutableStateFlow(CreateListingUiState())
    val uiState: StateFlow<CreateListingUiState> = _uiState

    // ----------------- Update form fields -----------------
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

    // Update tier
    fun updateTier(tier: ListingTier) {
        _uiState.value = _uiState.value.copy(tier = tier)

        // If current images exceed new tier's max, remove excess images
        val currentImages = _uiState.value.selectedImages
        if (currentImages.size > tier.maxImages) {
            val updatedImages = currentImages.take(tier.maxImages)
            _uiState.value = _uiState.value.copy(selectedImages = updatedImages)
            Log.d(TAG, "📸 Tier changed to ${tier.displayName}. Images reduced to ${updatedImages.size}/${tier.maxImages}")
        } else {
            Log.d(TAG, "📸 Tier changed to ${tier.displayName}. Max images: ${tier.maxImages}")
        }
    }

    // ----------------- Image handling -----------------
    fun addImages(uris: List<Uri>) {
        val currentImages = _uiState.value.selectedImages
        val maxImages = _uiState.value.tier.maxImages // NOW WORKS
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

    // ----------------- Reset form -----------------
    fun resetForm() {
        _uiState.value = CreateListingUiState()
        Log.d(TAG, "🔄 Form reset")
    }

    // ----------------- Create Listing -----------------
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
                Log.w(TAG, "⚠️ Breed is blank")
                _uiState.value = state.copy(error = "Breed is required")
                return
            }
            state.age.isBlank() -> {
                Log.w(TAG, "⚠️ Age is blank")
                _uiState.value = state.copy(error = "Age is required")
                return
            }
            state.price.isBlank() -> {
                Log.w(TAG, "⚠️ Price is blank")
                _uiState.value = state.copy(error = "Price is required")
                return
            }
            state.location.isBlank() -> {
                Log.w(TAG, "⚠️ Location is blank")
                _uiState.value = state.copy(error = "Location is required")
                return
            }
            state.selectedImages.isEmpty() -> {
                Log.w(TAG, "⚠️ No images selected")
                _uiState.value = state.copy(error = "At least one image is required")
                return
            }
        }

        // ✅ CRITICAL FIX: Convert price to Long (to match Firestore number type)
        val priceAsLong = state.price.toLongOrNull()
        if (priceAsLong == null) {
            Log.w(TAG, "⚠️ Price is not a valid number: ${state.price}")
            _uiState.value = state.copy(error = "Valid price is required (numbers only)")
            return
        }

        Log.d(TAG, "✅ Validation passed")
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "📤 Uploading ${state.selectedImages.size} images to Supabase...")

                // Upload images to Supabase
                val uploadedUrls = SupabaseStorageHelper.uploadImagesWithProgress(
                    context,
                    state.selectedImages,
                    listingId = java.util.UUID.randomUUID().toString()
                ) { overallProgress ->
                    val progressMap = state.selectedImages.associateWith { overallProgress }
                    _uiState.value = _uiState.value.copy(uploadProgress = progressMap)
                    Log.d(TAG, "📊 Upload progress: ${(overallProgress * 100).toInt()}%")
                }

                if (uploadedUrls.isEmpty()) {
                    Log.e(TAG, "❌ Image upload failed - no URLs returned")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Image upload failed"
                    )
                    return@launch
                }

                Log.d(TAG, "✅ Images uploaded successfully: ${uploadedUrls.size} URLs")
                uploadedUrls.forEachIndexed { index, url ->
                    Log.d(TAG, "   Image $index: ${url.take(50)}...")
                }

                // ✅ Create listing object with LONG price and STRING tier
                val listing = Listing(
                    id = "", // Will be set by repository
                    user_id = userId,
                    breed = state.breed,
                    age = state.age,
                    price = priceAsLong,  // ✅ Long, not String
                    location = state.location,
                    full_details = state.fullDetails,
                    deworming = state.deworming,
                    vaccination_status = state.vaccinated,
                    image_urls = uploadedUrls,
                    is_active = true,
                    created_at = Timestamp.now(),
                    listingTier = state.tier.name  // ✅ Save as String: "FREE", "BASIC", etc.
                )

                Log.d(TAG, "💾 Creating listing in Firestore...")
                Log.d(TAG, "   Breed: ${listing.breed}")
                Log.d(TAG, "   Price: ${listing.price} (Long)")
                Log.d(TAG, "   User ID: ${listing.user_id}")
                Log.d(TAG, "   Images: ${listing.image_urls.size}")
                Log.d(TAG, "   Active: ${listing.is_active}")
                Log.d(TAG, "   Tier: ${listing.listingTier} (String)")

                // Save to Firestore
                val success = listingRepository.createListing(context, listing, state.selectedImages)

                if (success) {
                    Log.d(TAG, "✅✅✅ LISTING CREATED SUCCESSFULLY! ✅✅✅")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                } else {
                    Log.e(TAG, "❌ Repository returned false - listing creation failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create listing. Please try again."
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during listing creation: ${e.message}", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
}