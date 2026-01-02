package com.example.coded.models

import android.net.Uri
import java.util.UUID

data class ServiceJob(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val images: List<Uri> = emptyList(),
    val location: String = "",
    val budgetAmount: Double? = null,
    val vegetationType: String = "",
    val state: JobState = JobState.Created,
    val clientId: String = "",
    val providerId: String? = null,
    val providerPayout: Double? = null,
    val escrowAmount: Double? = null,
    val stateHistory: List<StateTransition> = emptyList()
)

data class StateTransition(
    val from: JobState? = null,
    val to: JobState,
    val timestamp: Long = System.currentTimeMillis()
)