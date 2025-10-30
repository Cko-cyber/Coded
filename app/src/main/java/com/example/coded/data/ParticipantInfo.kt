package com.example.coded.data

import com.google.firebase.Timestamp

data class ParticipantInfo(
    val userId: String = "",
    val name: String = "",
    val profilePic: String = "",
    val phone: String = "",
    val lastSeen: Timestamp? = null,
    val isOnline: Boolean = false
)