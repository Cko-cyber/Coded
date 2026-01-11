package com.example.coded.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class JobRepository(private val context: Context? = null) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "JobRepository"

    private val jobsRef = db.collection("jobs")
    private val usersRef = db.collection("users")
    private val providersRef = db.collection("providers")
    private val transactionsRef = db.collection("transactions")

    /**
     * ✅ Create a new job - ALLOWS ANONYMOUS USERS
     */
    suspend fun createJob(
        serviceType: String,
        description: String,
        locationAddress: String,
        locationLat: Double?,
        locationLng: Double?,
        estimatedArea: Double? = null,
        vegetationType: String? = null,
        growthStage: String? = null,
        terrainType: String? = null,
        serviceVariant: String? = null,
        priceBreakdown: JobPriceBreakdown,
        imageUris: List<Uri> = emptyList(),
        mobileMoneyProvider: String,
        mobileMoneyNumber: String,
        region: String? = null,
        town: String? = null,
        needsDisposal: Boolean = false,
        isUrgent: Boolean = false,
        preferredDate: Timestamp? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Generate unique ID for anonymous user
            val userId = auth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}_${(1000..9999).random()}"

            // 2. Client info for anonymous user
            val clientName = "Anonymous Client"
            val clientPhone = mobileMoneyNumber
            val clientEmail = "guest@oasis.com"

            // 3. Generate job ID
            val jobId = "JOB-${System.currentTimeMillis()}-${(1000..9999).random()}"

            // 4. Determine region and town
            val finalRegion = region ?: determineEswatiniRegion(locationLat, locationLng)
            val finalTown = town ?: determineEswatiniTown(locationLat, locationLng)

            // 5. Generate job title
            val jobTitle = generateJobTitle(serviceType, estimatedArea, serviceVariant)

            // 6. Create job document
            val job = Job(
                id = jobId,
                clientId = userId,
                clientName = clientName,
                clientPhone = clientPhone,
                clientEmail = clientEmail,
                serviceType = serviceType,
                serviceVariant = serviceVariant,
                title = jobTitle,
                description = description,
                locationAddress = locationAddress,
                locationLat = locationLat,
                locationLng = locationLng,
                estimatedArea = estimatedArea,
                vegetationType = vegetationType,
                growthStage = growthStage,
                terrainType = terrainType,
                needsDisposal = needsDisposal,
                isUrgent = isUrgent,
                preferredDate = preferredDate,
                basePrice = priceBreakdown.basePrice,
                vegetationSurcharge = priceBreakdown.vegetationSurcharge,
                growthSurcharge = priceBreakdown.growthSurcharge,
                terrainSurcharge = priceBreakdown.terrainSurcharge,
                serviceSurcharge = priceBreakdown.serviceSurcharge,
                disposalFee = priceBreakdown.disposalFee,
                travelFee = priceBreakdown.travelFee,
                urgencyFee = priceBreakdown.urgencyFee,
                subtotal = priceBreakdown.subtotal,
                mobileMoneyFee = priceBreakdown.mobileMoneyFee,
                vat = priceBreakdown.vat,
                totalAmount = priceBreakdown.totalAmount,
                estimatedHours = priceBreakdown.estimatedHours,
                paymentMethod = "mobile_money",
                mobileMoneyProvider = mobileMoneyProvider,
                mobileMoneyNumber = mobileMoneyNumber,
                paymentStatus = "pending",
                status = "draft",
                imageUrls = emptyList(), // Will upload images separately if needed
                region = finalRegion,
                town = finalTown,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // 7. Save to Firestore
            jobsRef.document(jobId).set(job.toMap()).await()

            Log.d(TAG, "✅ Job created successfully: $jobId")
            Log.d(TAG, "   Client: Anonymous")
            Log.d(TAG, "   Service: $serviceType - $jobTitle")
            Log.d(TAG, "   Location: $locationAddress ($finalRegion, $finalTown)")
            Log.d(TAG, "   Total Amount: E${priceBreakdown.totalAmount}")
            Log.d(TAG, "   Mobile Money: $mobileMoneyProvider - $mobileMoneyNumber")

            // 8. Create initial transaction record
            createTransactionRecord(jobId, userId, priceBreakdown.totalAmount, "pending")

            Result.success(jobId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create job: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Update job payment status - SIMULATE FOR TESTING
     */
    suspend fun updateJobPayment(
        jobId: String,
        paymentStatus: String = "paid",
        transactionId: String,
        paymentReference: String,
        providerName: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // For testing, auto-assign a test provider
            val testProviderId = "test_provider_001"
            val testProviderName = "Test Provider"
            val testProviderPhone = "761234567"
            val testProviderRating = 4.5

            val updates = mutableMapOf<String, Any>(
                "paymentStatus" to "paid",
                "transactionId" to transactionId,
                "paymentReference" to paymentReference,
                "paidAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
                "status" to "assigned", // Auto-assign for testing
                "providerId" to testProviderId,
                "providerName" to testProviderName,
                "providerPhone" to testProviderPhone,
                "providerRating" to testProviderRating,
                "assignedAt" to Timestamp.now(),
                "assignedBy" to "system",
                "assignedByName" to "Auto-System",
                "isTestJob" to true // Flag for test jobs
            )

            jobsRef.document(jobId).update(updates).await()

            Log.d(TAG, "✅ Payment simulated successfully for job $jobId")
            Log.d(TAG, "   Auto-assigned to: $testProviderName")
            Log.d(TAG, "   Status: assigned (for testing)")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update payment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Get test jobs for providers (for testing)
     */
    suspend fun getTestJobsForProviders(): Result<List<Job>> = withContext(Dispatchers.IO) {
        try {
            // Query for test jobs that are assigned
            val query = jobsRef
                .whereEqualTo("isTestJob", true)
                .whereEqualTo("status", "assigned")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)

            val snapshot = query.get().await()

            val jobs = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Job::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing job ${doc.id}", e)
                    null
                }
            }

            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ✅ Create test jobs for provider testing
     */
    suspend fun createTestJobsForProviders(count: Int = 3): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val jobIds = mutableListOf<String>()

            for (i in 1..count) {
                val jobId = "TEST_JOB_${System.currentTimeMillis()}_$i"

                val testJob = Job(
                    id = jobId,
                    clientId = "test_client_$i",
                    clientName = "Test Client $i",
                    clientPhone = "7612 3456",
                    clientEmail = "test$i@example.com",
                    serviceType = when (i % 3) {
                        0 -> "grass_cutting"
                        1 -> "cleaning"
                        else -> "plumbing"
                    },
                    serviceVariant = when (i % 3) {
                        0 -> null
                        1 -> "house_cleaning"
                        else -> "leaking_tap"
                    },
                    title = "Test Job $i - ${when (i % 3) {
                        0 -> "Grass Cutting"
                        1 -> "House Cleaning"
                        else -> "Plumbing"
                    }}",
                    description = "This is a test job created for provider testing. Please ignore.",
                    locationAddress = "Test Location $i, Eswatini",
                    locationLat = -26.305 + (i * 0.01),
                    locationLng = 31.130 + (i * 0.01),
                    estimatedArea = (50 + i * 10).toDouble(),
                    totalAmount = (100 + i * 50).toDouble(),
                    paymentStatus = "paid",
                    status = "assigned",
                    providerId = "test_provider_001",
                    providerName = "Test Provider",
                    providerPhone = "761234567",
                    providerRating = 4.5,
                    region = when (i % 4) {
                        0 -> "Hhohho"
                        1 -> "Manzini"
                        2 -> "Lubombo"
                        else -> "Shiselweni"
                    },
                    town = when (i % 3) {
                        0 -> "Mbabane"
                        1 -> "Manzini"
                        else -> "Matsapha"
                    },
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    assignedAt = Timestamp.now(),
                    assignedBy = "system",
                    assignedByName = "Auto-System"
                )

                jobsRef.document(jobId).set(testJob.toMap()).await()
                jobIds.add(jobId)

                Log.d(TAG, "✅ Created test job: $jobId")
            }

            Result.success(jobIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Rest of the helper methods remain the same...
    private fun determineEswatiniRegion(lat: Double?, lng: Double?): String {
        if (lat == null || lng == null) return "Unknown"
        return when {
            lat > -26.0 && lng < 31.5 -> "Hhohho"
            lat > -26.5 && lat < -26.0 && lng > 31.0 && lng < 31.5 -> "Manzini"
            lat > -26.5 && lng > 31.5 -> "Lubombo"
            lat < -26.5 -> "Shiselweni"
            else -> "Manzini"
        }
    }

    private fun determineEswatiniTown(lat: Double?, lng: Double?): String {
        if (lat == null || lng == null) return "Unknown"
        return when {
            lat > -26.3 && lat < -26.2 && lng > 31.1 && lng < 31.2 -> "Mbabane"
            lat > -26.5 && lat < -26.4 && lng > 31.3 && lng < 31.4 -> "Manzini"
            lat > -26.52 && lat < -26.5 && lng > 31.3 && lng < 31.32 -> "Matsapha"
            else -> "Rural Area"
        }
    }

    private fun generateJobTitle(
        serviceType: String,
        estimatedArea: Double?,
        serviceVariant: String?
    ): String {
        val serviceName = when (serviceType) {
            "grass_cutting" -> "Grass Cutting"
            "yard_clearing" -> "Yard Clearing"
            "gardening" -> "Gardening"
            "tree_felling" -> "Tree Felling"
            "cleaning" -> "Cleaning"
            "plumbing" -> "Plumbing"
            "electrical" -> "Electrical"
            "maintenance" -> "Maintenance"
            "dstv_installation" -> "DSTV Installation"
            else -> "Service"
        }

        val variantText = when (serviceVariant) {
            "leaking_tap" -> " - Leaking Tap"
            "blocked_drain" -> " - Blocked Drain"
            "socket_repair" -> " - Socket Repair"
            "light_installation" -> " - Light Installation"
            "multi_room" -> " - Multi-room"
            else -> ""
        }

        return if (estimatedArea != null) {
            "$serviceName$variantText - ${"%.0f".format(estimatedArea)} sq m"
        } else {
            "$serviceName$variantText"
        }
    }

    private suspend fun createTransactionRecord(
        jobId: String,
        userId: String,
        amount: Double,
        status: String
    ) {
        try {
            val transactionId = "TXN-${System.currentTimeMillis()}-${(1000..9999).random()}"

            val transaction = mapOf(
                "id" to transactionId,
                "jobId" to jobId,
                "userId" to userId,
                "amount" to amount,
                "currency" to "SZL",
                "paymentMethod" to "mobile_money",
                "status" to status,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            transactionsRef.document(transactionId).set(transaction).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create transaction record", e)
        }
    }
}