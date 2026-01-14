package com.example.coded.utils

import com.example.coded.CodedApplication
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Instant

object TestHelper {
    private val supabase = CodedApplication.supabase

    suspend fun createTestJobsForProviders(count: Int = 5) {
        val jobsRef = supabase.from("jobs")

        for (i in 1..count) {
            val jobId = "TEST_JOB_${System.currentTimeMillis()}_$i"

            val testJob = mapOf(
                "id" to jobId,
                "client_id" to "test_client_$i",
                "client_name" to "Test Client $i",
                "service_type" to when (i % 3) {
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
                "location_address" to "Test Location $i, Mbabane, Eswatini",
                "total_amount" to (100 + i * 50).toDouble(),
                "payment_status" to "paid",
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
                "created_at" to Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString(),
                "updated_at" to Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString(),
                "is_test_job" to true
            )

            jobsRef.insert(testJob)
        }
    }

    suspend fun clearTestJobs() {
        // Note: Supabase delete requires filtering
        // This would be better done via a Postgres function or manual deletion
        val jobsRef = supabase.from("jobs")

        val testJobs = jobsRef.select() {
            filter {
                eq("is_test_job", true)
            }
        }.decodeList<Map<String, Any>>()

        testJobs.forEach { job ->
            val id = job["id"] as? String
            if (id != null) {
                jobsRef.delete {
                    filter {
                        eq("id", id)
                    }
                }
            }
        }
    }
}