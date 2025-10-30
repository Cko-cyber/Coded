package com.example.coded.data

import com.google.firebase.Timestamp

data class ListingContext(
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImage: String = "",
    val listingPrice: Double = 0.0,
    val messageCount: Int = 0,
    val lastMessageTime: Timestamp? = null
)