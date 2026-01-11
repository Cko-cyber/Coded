package com.example.coded.data

import com.google.firebase.Timestamp

data class Payment(
    val id: String = "",
    val jobId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "SZL",
    val paymentMethod: String = "mobile_money",
    val mobileMoneyProvider: String = "",
    val mobileNumber: String = "",
    val reference: String = "",
    val status: String = "pending", // pending, processing, completed, failed
    val providerResponse: String? = null,
    val responseCode: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null
)