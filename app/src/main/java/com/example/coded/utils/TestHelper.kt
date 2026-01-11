package com.example.coded.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

object TestHelper {
    private val db = FirebaseFirestore.getInstance()

    suspend fun createTestJobsForProviders(count: Int = 5) {
        val jobsRef = db.collection("jobs")

        for (i in 1..count) {
            val jobId = "TEST_JOB_${System.currentTimeMillis()}_$i"

            val testJob = mapOf(
                "id" to jobId,
                "clientId" to "test_client_$i",
                "clientName" to "Test Client $i",
                "serviceType" to when (i % 3) {
                    0 -> "grass_cutting"
                    1 -> "cleaning"
                    else -> "plumbing"
                },
                "title" to "Test Job $i - ${when (i % 3) {
                    0 -> "Grass Cutting"
                    1 -> "House Cleaning"
                    else -> "Plumbing Repair"
                }}",
                "description" to "This is a test job created for provider testing. Area: ${50 + i * 10} sq m",
                "locationAddress" to "Test Location $i, Mbabane, Eswatini",
                "totalAmount" to (100 + i * 50).toDouble(),
                "paymentStatus" to "paid",
                "status" to "pending_assignment",
                "region" to when (i % 4) {
                    0 -> "Hhohho"
                    1 -> "Manzini"
                    2 -> "Lubombo"
                    else -> "Shiselweni"
                },
                "town" to when (i % 3) {
                    0 -> "Mbabane"
                    1 -> "Manzini"
                    else -> "Matsapha"
                },
                "createdAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now(),
                "isTestJob" to true
            )

            jobsRef.document(jobId).set(testJob).await()
        }
    }

    suspend fun clearTestJobs() {
        val jobsRef = db.collection("jobs")
        val query = jobsRef.whereEqualTo("isTestJob", true).get().await()

        for (doc in query.documents) {
            jobsRef.document(doc.id).delete().await()
        }
    }
}