package com.example.coded.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ParticipantInfo(
    val userId: String = "",
    val name: String = "",
    val profilePic: String = "",
    val phone: String = "",
    val lastSeen: Timestamp? = null,

    // Add PropertyName annotations to match Firestore field names
    @PropertyName("isOnline")
    val isOnline: Boolean = false,

    @PropertyName("fcm_token")
    val fcm_token: String = ""
)