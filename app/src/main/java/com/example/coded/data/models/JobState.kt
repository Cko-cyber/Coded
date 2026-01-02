package com.herdmat.coded.models

sealed class JobState {
    object Created : JobState()
    object Funded : JobState()
    object Assigned : JobState()
    object Accepted : JobState()
    object InProgress : JobState()
    object Completed : JobState()
    data class Verified(val autoVerified: Boolean = false) : JobState()
    object PaidOut : JobState()
}