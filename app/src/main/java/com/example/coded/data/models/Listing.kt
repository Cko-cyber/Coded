package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Listing(
    @DocumentId
    val id: String = "",
    val user_id: String = "",
    val breed: String = "",
    val age: String = "",
    val price: Long = 0,
    val location: String = "",
    val deworming: String = "",
    val vaccination_status: String = "",
    val full_details: String = "",
    val image_urls: List<String> = emptyList(),
    @field:PropertyName("is_active")
    val isActive: Boolean = true,
    @PropertyName("listingTier")
    val listingTier: String = "FREE",
    val userId: String = "",
    val is_active: Boolean = true,
    val created_at: Timestamp = Timestamp.now()
) {
    companion object {
        // Move constants outside the fromMap function
        const val FIELD_IS_ACTIVE = "is_active"
        const val FIELD_USER_ID = "user_id"

        fun fromMap(id: String, data: Map<String, Any>): Listing {
            return Listing(
                id = id,
                user_id = data["user_id"] as? String ?: "",
                breed = data["breed"] as? String ?: "",
                age = data["age"] as? String ?: "",
                price = (data["price"] as? Number)?.toLong() ?: 0L,
                location = data["location"] as? String ?: "",
                deworming = data["deworming"] as? String ?: "",
                vaccination_status = data["vaccination_status"] as? String ?: "",
                full_details = data["full_details"] as? String ?: "",
                // FIXED: Safe cast for image_urls
                image_urls = (data["image_urls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                listingTier = data["listingTier"] as? String ?: "FREE",
                is_active = data["is_active"] as? Boolean ?: true,
                created_at = data["created_at"] as? Timestamp ?: Timestamp.now()
            )
        }
    }

    fun getDisplayPrice(): String {
        return "E $price"
    }

    fun getTierEnum(): ListingTier {
        return try {
            ListingTier.valueOf(listingTier)
        } catch (e: Exception) {
            ListingTier.FREE
        }
    }

    fun isViewable(): Boolean {
        return is_active && image_urls.isNotEmpty()
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "user_id" to user_id,
            "breed" to breed,
            "age" to age,
            "price" to price,
            "location" to location,
            "deworming" to deworming,
            "vaccination_status" to vaccination_status,
            "full_details" to full_details,
            "image_urls" to image_urls,
            "listingTier" to listingTier,
            "is_active" to is_active,
            "created_at" to created_at
        )
    }
}

// Add ListingTier enum here
enum class ListingTier(val displayName: String, val maxImages: Int) {
    FREE("Free", 3),
    BASIC("Basic", 6),
    BULK("Bulk", 6),
    PREMIUM("Premium", 6);

    companion object {
        fun fromString(value: String): ListingTier {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: FREE
        }
    }
}