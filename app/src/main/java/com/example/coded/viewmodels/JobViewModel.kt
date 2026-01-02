package com.herdmat.coded.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmat.coded.models.JobState
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {
    fun createJob(/* params */) {
        viewModelScope.launch {
            // Backend call, set Created -> Funded
        }
    }

    fun assignJob(jobId: String) {
        viewModelScope.launch {
            // System assign, update Assigned
        }
    }

    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            // Update Accepted
        }
    }

    fun startJob(jobId: String) {
        viewModelScope.launch {
            // Update InProgress
        }
    }

    fun completeJob(jobId: String) {
        viewModelScope.launch {
            // Update Completed
        }
    }

    fun verifyJob(/* params */) {
        viewModelScope.launch {
            // Update Verified
        }
    }

    fun payoutJob(jobId: String) {
        viewModelScope.launch {
            // Update PaidOut
        }
    }

    // Enforce sequential state transitions
    private fun validateTransition(current: JobState, next: JobState): Boolean {
        return when (current) {
            JobState.Created -> next == JobState.Funded
            JobState.Funded -> next == JobState.Assigned
            JobState.Assigned -> next == JobState.Accepted
            JobState.Accepted -> next == JobState.InProgress
            JobState.InProgress -> next == JobState.Completed
            JobState.Completed -> next is JobState.Verified
            is JobState.Verified -> next == JobState.PaidOut
            else -> false
        }
    }
}