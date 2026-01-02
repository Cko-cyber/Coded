package com.example.coded.managers

import com.example.coded.models.JobState
import com.example.coded.models.ServiceJob
import com.example.coded.models.StateTransition
import com.google.gson.Gson
import kotlin.reflect.KClass

class JobManager {
    private val gson = Gson()
    private val jobs = mutableMapOf<String, ServiceJob>()

    private fun <T : Any> String.toObject(clazz: KClass<T>): T? {
        return try {
            gson.fromJson(this, clazz.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getJob(jobId: String): ServiceJob? {
        return jobs[jobId]
    }

    fun updateJobState(jobId: String, newState: JobState) {
        val job = jobs[jobId] ?: return
        if (!isValidTransition(job.state, newState)) return
        val transition = StateTransition(job.state, newState)
        val updatedJob = job.copy(
            state = newState,
            stateHistory = job.stateHistory + transition
        )
        jobs[jobId] = updatedJob
    }

    fun getStateTimestamps(jobId: String): Map<JobState, Long> {
        val job = jobs[jobId] ?: return emptyMap()
        return job.stateHistory.associate { it.to to it.timestamp }
    }

    fun acceptJob(jobId: String, providerId: String) {
        val job = jobs[jobId] ?: return
        if (job.state == JobState.Assigned) {
            updateJobState(jobId, JobState.Accepted)
            jobs[jobId] = job.copy(providerId = providerId)
        }
    }

    fun startJob(jobId: String) {
        val job = jobs[jobId] ?: return
        if (job.state == JobState.Accepted) {
            updateJobState(jobId, JobState.InProgress)
        }
    }

    fun completeJob(jobId: String) {
        val job = jobs[jobId] ?: return
        if (job.state == JobState.InProgress) {
            updateJobState(jobId, JobState.Completed)
        }
    }

    fun verifyJob(jobId: String, autoVerified: Boolean = false) {
        val job = jobs[jobId] ?: return
        if (job.state == JobState.Completed) {
            updateJobState(jobId, JobState.Verified(autoVerified))
        }
    }

    fun payoutJob(jobId: String) {
        val job = jobs[jobId] ?: return
        if (job.state is JobState.Verified) {
            updateJobState(jobId, JobState.PaidOut)
        }
    }

    fun createJob(job: ServiceJob) {
        jobs[job.id] = job
    }

    fun assignJob(jobId: String, providerId: String) {
        val job = jobs[jobId] ?: return
        if (job.state == JobState.Funded) {
            updateJobState(jobId, JobState.Assigned)
            jobs[jobId] = job.copy(providerId = providerId)
        }
    }

    fun fundJob(jobId: String, escrowAmount: Double) {
        val job = jobs[jobId] ?: return
        if (job.state == JobState.Created) {
            updateJobState(jobId, JobState.Funded)
            jobs[jobId] = job.copy(escrowAmount = escrowAmount)
        }
    }

    private fun isValidTransition(current: JobState, next: JobState): Boolean {
        return when (current) {
            JobState.Created -> next == JobState.Funded
            JobState.Funded -> next == JobState.Assigned
            JobState.Assigned -> next == JobState.Accepted
            JobState.Accepted -> next == JobState.InProgress
            JobState.InProgress -> next == JobState.Completed
            JobState.Completed -> next is JobState.Verified
            is JobState.Verified -> next == JobState.PaidOut
            JobState.PaidOut -> false
        }
    }

    fun getJobByClient(clientId: String): List<ServiceJob> {
        return jobs.values.filter { it.clientId == clientId }
    }

    fun getJobState(jobId: String): JobState? {
        return jobs[jobId]?.state
    }
}