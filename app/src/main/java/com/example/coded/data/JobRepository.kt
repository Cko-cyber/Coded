package com.example.coded.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.coded.CodedApplication
import com.google.firebase.Timestamp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.time.Instant
/**
 * Repository for Job operations with Supabase
 */
class JobRepository(private val context: Context) {
    private val TAG = "JobRepository"
    private val supabase = CodedApplication.supabase
    private val storageHelper = SupabaseStorageHelper(context)

    /**
     * Create a new job in Supabase
     */
    suspend fun createJob(
        serviceType: String,
        description: String,
        locationAddress: String,
        locationLat: Double?,
        locationLng: Double?,
        estimatedArea: Double?,
        vegetationType: String?,
        growthStage: String?,
        terrainType: String?,
        serviceVariant: String?,
        priceBreakdown: JobPriceBreakdown,
        imageUris: List<Uri>,
        mobileMoneyProvider: String,
        mobileMoneyNumber: String,
        needsDisposal: Boolean,
        isUrgent: Boolean,
        preferredDate: Timestamp?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Upload images first
            val imageUrls = if (imageUris.isNotEmpty()) {
                val uploadResult = storageHelper.uploadJobImages(
                    imageUris = imageUris,
                    userId = "anonymous_${System.currentTimeMillis()}"
                )
                uploadResult.getOrNull() ?: emptyList()
            } else {
                emptyList()
            }

            // Generate job ID
            val jobId = "JOB_${System.currentTimeMillis()}_${Random().nextInt(9999)}"

            // Create job data
            val jobData = mapOf(
                "id" to jobId,
                "client_name" to "Anonymous Client",
                "client_phone" to mobileMoneyNumber,
                "service_type" to serviceType,
                "service_variant" to serviceVariant,
                "title" to "$serviceType - ${locationAddress.take(30)}",
                "description" to description,
                "location_address" to locationAddress,
                "location_lat" to locationLat,
                "location_lng" to locationLng,
                "estimated_area" to estimatedArea,
                "vegetation_type" to vegetationType,
                "growth_stage" to growthStage,
                "terrain_type" to terrainType,
                "needs_disposal" to needsDisposal,
                "is_urgent" to isUrgent,
                "preferred_date" to preferredDate?.let {
                    Instant.ofEpochMilli(it.seconds * 1000).toString()
                },
                "base_price" to priceBreakdown.basePrice,
                "vegetation_surcharge" to priceBreakdown.vegetationSurcharge,
                "growth_surcharge" to priceBreakdown.growthSurcharge,
                "terrain_surcharge" to priceBreakdown.terrainSurcharge,
                "service_surcharge" to priceBreakdown.serviceSurcharge,
                "disposal_fee" to priceBreakdown.disposalFee,
                "travel_fee" to priceBreakdown.travelFee,
                "urgency_fee" to priceBreakdown.urgencyFee,
                "subtotal" to priceBreakdown.subtotal,
                "mobile_money_fee" to priceBreakdown.mobileMoneyFee,
                "vat" to priceBreakdown.vat,
                "total_amount" to priceBreakdown.totalAmount,
                "estimated_hours" to priceBreakdown.estimatedHours,
                "mobile_money_provider" to mobileMoneyProvider,
                "mobile_money_number" to mobileMoneyNumber,
                "payment_status" to "pending",
                "status" to "draft",
                "image_urls" to imageUrls,
                "created_at" to Instant.ofEpochMilli(System.currentTimeMillis()).toString(),
                "updated_at" to Instant.ofEpochMilli(System.currentTimeMillis()).toString(),
                "is_test_job" to false
            )

            // Insert into Supabase
            supabase.from("jobs").insert(jobData)

            Log.d(TAG, "✅ Job created successfully: $jobId")
            Result.success(jobId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create job", e)
            Result.failure(e)
        }
    }

    /**
     * Update job payment status
     */
    suspend fun updateJobPayment(
        jobId: String,
        paymentStatus: String,
        transactionId: String,
        paymentReference: String,
        providerName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("jobs").update({
                set("payment_status", paymentStatus)
                set("transaction_id", transactionId)
                set("payment_reference", paymentReference)
                set("paid_at", Instant.ofEpochMilli(System.currentTimeMillis()).toString())
                set("status", "pending_assignment")
                set("updated_at", Instant.ofEpochMilli(System.currentTimeMillis()).toString())
            }) {
                filter {
                    eq("id", jobId)
                }
            }

            Log.d(TAG, "✅ Job payment updated: $jobId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update job payment", e)
            Result.failure(e)
        }
    }

    /**
     * Get test jobs for providers
     */
    suspend fun getTestJobsForProviders(): Result<List<Job>> = withContext(Dispatchers.IO) {
        try {
            val jobs = supabase.from("jobs")
                .select() {
                    filter {
                        eq("is_test_job", true)
                        eq("status", "pending_assignment")
                    }
                }
                .decodeList<Map<String, Any?>>()
                .map { mapToJob(it) }

            Log.d(TAG, "✅ Retrieved ${jobs.size} test jobs")
            Result.success(jobs)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get test jobs", e)
            Result.success(emptyList()) // Return empty list on error
        }
    }

    /**
     * Create test jobs for provider testing
     */
    suspend fun createTestJobsForProviders(count: Int = 3): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val jobIds = mutableListOf<String>()

            for (i in 1..count) {
                val jobId = "TEST_JOB_${System.currentTimeMillis()}_$i"

                val testJob = mapOf(
                    "id" to jobId,
                    "client_name" to "Test Client $i",
                    "client_phone" to "76123456",
                    "client_email" to "test$i@example.com",
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
                    "created_at" to Instant.ofEpochMilli(System.currentTimeMillis()).toString(),
                    "updated_at" to Instant.ofEpochMilli(System.currentTimeMillis()).toString(),
                    "is_test_job" to true,
                    "is_urgent" to (i % 2 == 0)
                )

                supabase.from("jobs").insert(testJob)
                jobIds.add(jobId)
            }

            Log.d(TAG, "✅ Created ${jobIds.size} test jobs")
            Result.success(jobIds)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create test jobs", e)
            Result.failure(e)
        }
    }

    /**
     * Convert Supabase map to Job data class
     */
    private fun mapToJob(data: Map<String, Any?>): Job {
        return Job(
            id = data["id"] as? String ?: "",
            clientId = data["client_id"] as? String ?: "",
            clientName = data["client_name"] as? String ?: "",
            clientPhone = data["client_phone"] as? String ?: "",
            clientEmail = data["client_email"] as? String ?: "",
            serviceType = data["service_type"] as? String ?: "",
            serviceVariant = data["service_variant"] as? String,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            locationAddress = data["location_address"] as? String ?: "",
            locationLat = (data["location_lat"] as? Number)?.toDouble(),
            locationLng = (data["location_lng"] as? Number)?.toDouble(),
            estimatedArea = (data["estimated_area"] as? Number)?.toDouble(),
            vegetationType = data["vegetation_type"] as? String,
            growthStage = data["growth_stage"] as? String,
            terrainType = data["terrain_type"] as? String,
            needsDisposal = data["needs_disposal"] as? Boolean ?: false,
            isUrgent = data["is_urgent"] as? Boolean ?: false,
            preferredDate = parseTimestamp(data["preferred_date"] as? String),
            basePrice = (data["base_price"] as? Number)?.toDouble() ?: 0.0,
            vegetationSurcharge = (data["vegetation_surcharge"] as? Number)?.toDouble() ?: 0.0,
            growthSurcharge = (data["growth_surcharge"] as? Number)?.toDouble() ?: 0.0,
            terrainSurcharge = (data["terrain_surcharge"] as? Number)?.toDouble() ?: 0.0,
            serviceSurcharge = (data["service_surcharge"] as? Number)?.toDouble() ?: 0.0,
            disposalFee = (data["disposal_fee"] as? Number)?.toDouble() ?: 0.0,
            travelFee = (data["travel_fee"] as? Number)?.toDouble() ?: 0.0,
            urgencyFee = (data["urgency_fee"] as? Number)?.toDouble() ?: 0.0,
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            platformFee = (data["platform_fee"] as? Number)?.toDouble() ?: 0.0,
            mobileMoneyFee = (data["mobile_money_fee"] as? Number)?.toDouble() ?: 0.0,
            vat = (data["vat"] as? Number)?.toDouble() ?: 0.0,
            totalAmount = (data["total_amount"] as? Number)?.toDouble() ?: 0.0,
            estimatedHours = (data["estimated_hours"] as? Number)?.toDouble() ?: 0.0,
            paymentMethod = data["payment_method"] as? String ?: "",
            mobileMoneyProvider = data["mobile_money_provider"] as? String ?: "",
            mobileMoneyNumber = data["mobile_money_number"] as? String ?: "",
            paymentStatus = data["payment_status"] as? String ?: "pending",
            status = data["status"] as? String ?: "draft",
            imageUrls = (data["image_urls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            region = data["region"] as? String ?: "",
            town = data["town"] as? String ?: "",
            createdAt = parseTimestamp(data["created_at"] as? String) ?: Timestamp.now(),
            updatedAt = parseTimestamp(data["updated_at"] as? String) ?: Timestamp.now(),
            transactionId = data["transaction_id"] as? String,
            paymentReference = data["payment_reference"] as? String,
            paidAt = parseTimestamp(data["paid_at"] as? String),
            providerId = data["provider_id"] as? String,
            providerName = data["provider_name"] as? String,
            providerPhone = data["provider_phone"] as? String,
            providerRating = (data["provider_rating"] as? Number)?.toDouble(),
            assignedBy = data["assigned_by"] as? String,
            assignedByName = data["assigned_by_name"] as? String,
            assignedAt = parseTimestamp(data["assigned_at"] as? String),
            assignmentNotes = data["assignment_notes"] as? String ?: "",
            deadline = parseTimestamp(data["deadline"] as? String),
            acceptedAt = parseTimestamp(data["accepted_at"] as? String),
            startedAt = parseTimestamp(data["started_at"] as? String),
            completionNotes = data["completion_notes"] as? String,
            completionImageUrls = (data["completion_images"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            completedAt = parseTimestamp(data["completed_at"] as? String),
            cancelledAt = parseTimestamp(data["cancelled_at"] as? String),
            cancelledBy = data["cancelled_by"] as? String,
            cancellationReason = data["cancellation_reason"] as? String ?: "",
            refundAmount = (data["refund_amount"] as? Number)?.toDouble(),
            refundStatus = data["refund_status"] as? String
        )
    }

    /**
     * Parse ISO timestamp string to Firebase Timestamp
     */
    private fun parseTimestamp(isoString: String?): Timestamp? {
        if (isoString == null) return null
        return try {
            val instant = Instant.parse(isoString)
            Timestamp(instant.epochSecond, 0)
        } catch (e: Exception) {
            null
        }
    }
}