package com.example.coded.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import android.content.Context
import com.example.coded.BuildConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.json.Json
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.Order


// ============================================
// SUPABASE CLIENT SETUP
// ============================================

object SupabaseClientManager {
    private lateinit var client: SupabaseClient

    fun initialize(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    fun get(): SupabaseClient = client
}

// ============================================
// DATA MODELS
// ============================================

@Serializable
data class Profile(
    val id: String,
    val phone_number: String? = null,
    val email: String? = null,
    val full_name: String? = null,
    val user_type: String, // client, provider, admin
    val avatar_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val last_seen: String? = null,
    val is_active: Boolean = true
)

@Serializable
data class AnonymousClient(
    val id: String? = null,
    val session_token: String,
    val device_hash: String? = null,
    val phone_number: String,
    val mobile_money_provider: String,
    val risk_score: Int = 0,
    val jobs_completed: Int = 0,
    val total_spent: Double = 0.0,
    val created_at: String? = null,
    val expires_at: String? = null,
    val converted_to_user_id: String? = null,
    val is_expired: Boolean = false
)

@Serializable
data class Provider(
    val id: String,
    val full_name: String,
    val phone_number: String,
    val email: String? = null,
    val id_number: String? = null,
    val profile_image_url: String? = null,
    val service_types: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val bio: String? = null,
    val location: String? = null,
    val region: String? = null,
    val town: String? = null,
    val address: String? = null,
    val is_verified: Boolean = false,
    val verification_status: String = "pending",
    val verified_at: String? = null,
    val rating: Double = 0.0,
    val total_ratings: Int = 0,
    val total_jobs: Int = 0,
    val completed_jobs: Int = 0,
    val cancelled_jobs: Int = 0,
    val acceptance_rate: Double = 0.0,
    val completion_rate: Double = 0.0,
    val average_response_time: Int = 0,
    val is_online: Boolean = false,
    val is_available: Boolean = true,
    val last_location_update: String? = null,
    val bank_account_name: String? = null,
    val bank_account_number: String? = null,
    val bank_name: String? = null,
    val mobile_money_number: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val onboarded_at: String? = null,
    val last_active_at: String? = null
)

@Serializable
data class Job(
    val id: String? = null,
    val job_number: String? = null,
    val client_id: String? = null,
    val anonymous_client_id: String? = null,
    val client_name: String,
    val client_phone: String,
    val client_email: String? = null,
    val service_type: String,
    val service_variant: String? = null,
    val title: String,
    val description: String,
    val location_address: String,
    val location: String? = null,
    val region: String? = null,
    val town: String? = null,
    val estimated_area: Double? = null,
    val vegetation_type: String? = null,
    val growth_stage: String? = null,
    val terrain_type: String? = null,
    val needs_disposal: Boolean = false,
    val is_urgent: Boolean = false,
    val preferred_date: String? = null,
    val base_price: Double = 0.0,
    val vegetation_surcharge: Double = 0.0,
    val growth_surcharge: Double = 0.0,
    val terrain_surcharge: Double = 0.0,
    val service_surcharge: Double = 0.0,
    val disposal_fee: Double = 0.0,
    val travel_fee: Double = 0.0,
    val urgency_fee: Double = 0.0,
    val subtotal: Double = 0.0,
    val platform_fee: Double = 0.0,
    val mobile_money_fee: Double = 0.0,
    val vat: Double = 0.0,
    val total_amount: Double = 0.0,
    val estimated_hours: Double = 0.0,
    val payment_method: String = "mobile_money",
    val mobile_money_provider: String? = null,
    val mobile_money_number: String? = null,
    val payment_status: String = "pending",
    val transaction_id: String? = null,
    val payment_reference: String? = null,
    val paid_at: String? = null,
    val provider_id: String? = null,
    val provider_name: String? = null,
    val provider_phone: String? = null,
    val provider_rating: Double? = null,
    val assigned_by: String? = null,
    val assigned_by_name: String? = null,
    val assigned_at: String? = null,
    val assignment_notes: String? = null,
    val deadline: String? = null,
    val status: String = "draft",
    val accepted_at: String? = null,
    val started_at: String? = null,
    val completed_at: String? = null,
    val verified_at: String? = null,
    val cancelled_at: String? = null,
    val cancelled_by: String? = null,
    val cancellation_reason: String? = null,
    val completion_notes: String? = null,
    val completion_images: List<String> = emptyList(),
    val refund_amount: Double? = null,
    val refund_status: String? = null,
    val image_urls: List<String> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_test_job: Boolean = false
)

@Serializable
data class JobSuggestion(
    val id: String? = null,
    val job_id: String,
    val provider_id: String,
    val proximity_score: Double = 0.0,
    val skill_match_score: Double = 0.0,
    val performance_score: Double = 0.0,
    val total_score: Double = 0.0,
    val distance_km: Double? = null,
    val is_viewed: Boolean = false,
    val viewed_at: String? = null,
    val is_accepted: Boolean = false,
    val accepted_at: String? = null,
    val is_rejected: Boolean = false,
    val rejected_at: String? = null,
    val rejection_reason: String? = null,
    val created_at: String? = null,
    val expires_at: String? = null
)

@Serializable
data class Rating(
    val id: String? = null,
    val job_id: String,
    val provider_id: String,
    val client_id: String? = null,
    val anonymous_client_id: String? = null,
    val rating: Int,
    val review: String? = null,
    val quality_rating: Int? = null,
    val professionalism_rating: Int? = null,
    val timeliness_rating: Int? = null,
    val is_published: Boolean = true,
    val is_verified: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class Transaction(
    val id: String? = null,
    val transaction_number: String? = null,
    val job_id: String,
    val client_id: String? = null,
    val anonymous_client_id: String? = null,
    val provider_id: String? = null,
    val amount: Double,
    val platform_fee: Double,
    val provider_payout: Double? = null,
    val payment_method: String,
    val mobile_money_provider: String? = null,
    val mobile_money_number: String? = null,
    val payment_reference: String? = null,
    val external_transaction_id: String? = null,
    val status: String = "pending",
    val created_at: String? = null,
    val processed_at: String? = null,
    val completed_at: String? = null,
    val paid_out_at: String? = null
)

// ============================================
// AUTHENTICATION REPOSITORY
// ============================================

class SupabaseAuthRepository(private val context: Context) {
    private val supabase = SupabaseClientManager.get()

    suspend fun createAnonymousSession(
        phoneNumber: String,
        mobileMoneyProvider: String,
        deviceHash: String?
    ): Result<AnonymousClient> {
        return try {
            val sessionToken = generateSessionToken()
            val expiresAt = calculateExpiryDate(72)

            val anonymousClient = AnonymousClient(
                session_token = sessionToken,
                device_hash = deviceHash,
                phone_number = phoneNumber,
                mobile_money_provider = mobileMoneyProvider,
                expires_at = expiresAt
            )

            val result = supabase.from("anonymous_clients")
                .insert(anonymousClient)
                .decodeSingle<AnonymousClient>()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginProviderWithPhone(
        phoneNumber: String,
        password: String
    ): Result<Provider> {
        return try {
            // Use OTP for phone authentication
            supabase.auth.signInWith(OTP) {
                phone = phoneNumber
            }

            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("No user ID")
            val provider = supabase.from("providers")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Provider>()

            Result.success(provider)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginProviderWithEmail(
        email: String,
        password: String
    ): Result<Provider> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("No user ID")
            val provider = supabase.from("providers")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Provider>()

            Result.success(provider)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginAdmin(email: String, password: String): Result<Profile> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("No user ID")
            val profile = supabase.from("profiles")
                .select() {
                    filter {
                        eq("id", userId)
                        eq("user_type", "admin")
                    }
                }
                .decodeSingle<Profile>()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            supabase.auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = supabase.auth.currentUserOrNull()

    private fun generateSessionToken(): String {
        return "ANON_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun calculateExpiryDate(hours: Int): String {
        val now = Clock.System.now()
        val expiry = now.plus(hours.hours)
        return expiry.toString()
    }
}

// ============================================
// JOB REPOSITORY
// ============================================

class SupabaseJobRepository(private val context: Context) {
    private val supabase = SupabaseClientManager.get()

    suspend fun createJob(
        job: Job,
        anonymousClientId: String? = null
    ): Result<Job> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id

            val jobData = job.copy(
                client_id = userId,
                anonymous_client_id = anonymousClientId,
                status = "pending_payment"
            )

            val created = supabase.from("jobs")
                .insert(jobData)
                .decodeSingle<Job>()

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateJobPayment(
        jobId: String,
        paymentStatus: String,
        transactionId: String,
        paymentReference: String
    ): Result<Job> {
        return try {
            val updated = supabase.from("jobs")
                .update({
                    set("payment_status", paymentStatus)
                    set("transaction_id", transactionId)
                    set("payment_reference", paymentReference)
                    set("paid_at", Clock.System.now().toString())
                    set("status", "pending_assignment")
                }) {
                    filter {
                        eq("id", jobId)
                    }
                }
                .decodeSingle<Job>()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSuggestedJobsForProvider(providerId: String): Result<List<Job>> {
        return try {
            val suggestions = supabase.from("job_suggestions")
                .select() {
                    filter {
                        eq("provider_id", providerId)
                        eq("is_accepted", false)
                        eq("is_rejected", false)
                    }
                }
                .decodeList<JobSuggestion>()

            val jobIds = suggestions.map { it.job_id }

            if (jobIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val jobs = supabase.from("jobs")
                .select() {
                    filter {
                        isIn("id", jobIds)
                        eq("status", "pending_assignment")
                    }
                }
                .decodeList<Job>()

            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToJobUpdates(jobId: String): Flow<Job> {
        return supabase.channel("job_updates_$jobId")
            .postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "jobs"
                filter("id", FilterOperator.EQ, jobId)
            }.map { change ->
                change.decodeRecord<Job>()
            }
    }

    suspend fun acceptJob(jobId: String, providerId: String): Result<Job> {
        return try {
            val updated = supabase.from("jobs")
                .update({
                    set("status", "accepted")
                    set("accepted_at", Clock.System.now().toString())
                }) {
                    filter {
                        eq("id", jobId)
                        eq("provider_id", providerId)
                    }
                }
                .decodeSingle<Job>()

            supabase.from("job_suggestions")
                .update({
                    set("is_accepted", true)
                    set("accepted_at", Clock.System.now().toString())
                }) {
                    filter {
                        eq("job_id", jobId)
                        eq("provider_id", providerId)
                    }
                }

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTestJobsForProviders(count: Int = 3): Result<List<String>> {
        return try {
            val jobIds = mutableListOf<String>()

            for (i in 1..count) {
                val jobId = "TEST_JOB_${System.currentTimeMillis()}_$i"

                val testJob = Job(
                    id = jobId,
                    client_id = "test_client_$i",
                    client_name = "Test Client $i",
                    client_phone = "7612 3456",
                    client_email = "test$i@example.com",
                    service_type = when (i % 3) {
                        0 -> "grass_cutting"
                        1 -> "cleaning"
                        else -> "plumbing"
                    },
                    service_variant = when (i % 3) {
                        0 -> null
                        1 -> "house_cleaning"
                        else -> "leaking_tap"
                    },
                    title = "Test Job $i",
                    description = "This is a test job for provider testing",
                    location_address = "Test Location $i, Manzini",
                    total_amount = (100 + i * 50).toDouble(),
                    payment_status = "paid",
                    status = "pending_assignment",
                    is_test_job = true
                )

                supabase.from("jobs").insert(testJob)
                jobIds.add(jobId)
            }

            Result.success(jobIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTestJobsForProviders(): Result<List<Job>> {
        return try {
            val jobs = supabase.from("jobs")
                .select() {
                    filter {
                        eq("is_test_job", true)
                        eq("status", "pending_assignment")
                    }
                }
                .decodeList<Job>()

            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllJobs(): Result<List<Job>> {
        return try {
            val jobs = supabase.from("jobs")
                .select()
                .decodeList<Job>()

            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJobById(jobId: String): Result<Job> {
        return try {
            val job = supabase.from("jobs")
                .select() {
                    filter {
                        eq("id", jobId)
                    }
                }
                .decodeSingle<Job>()

            Result.success(job)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ============================================
// PROVIDER REPOSITORY
// ============================================

class SupabaseProviderRepository(private val context: Context) {
    private val supabase = SupabaseClientManager.get()

    suspend fun getProviderById(providerId: String): Result<Provider> {
        return try {
            val provider = supabase.from("providers")
                .select() {
                    filter {
                        eq("id", providerId)
                    }
                }
                .decodeSingle<Provider>()

            Result.success(provider)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProviderLocation(
        providerId: String,
        location: String,
        region: String,
        town: String
    ): Result<Provider> {
        return try {
            val updated = supabase.from("providers")
                .update({
                    set("location", location)
                    set("region", region)
                    set("town", town)
                    set("last_location_update", Clock.System.now().toString())
                }) {
                    filter {
                        eq("id", providerId)
                    }
                }
                .decodeSingle<Provider>()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProviderAvailability(
        providerId: String,
        isAvailable: Boolean
    ): Result<Provider> {
        return try {
            val updated = supabase.from("providers")
                .update({
                    set("is_available", isAvailable)
                    set("last_active_at", Clock.System.now().toString())
                }) {
                    filter {
                        eq("id", providerId)
                    }
                }
                .decodeSingle<Provider>()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ============================================
// TRANSACTION REPOSITORY
// ============================================

class SupabaseTransactionRepository(private val context: Context) {
    private val supabase = SupabaseClientManager.get()

    suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            val created = supabase.from("transactions")
                .insert(transaction)
                .decodeSingle<Transaction>()

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionsForProvider(providerId: String): Result<List<Transaction>> {
        return try {
            val transactions = supabase.from("transactions")
                .select() {
                    filter {
                        eq("provider_id", providerId)
                    }
                    order("created_at", Order.DESCENDING)

                }
                .decodeList<Transaction>()

            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransactionStatus(
        transactionId: String,
        status: String
    ): Result<Transaction> {
        return try {
            val timestamp = Clock.System.now().toString()
            val updateData = mutableMapOf<String, Any?>(
                "status" to status
            )
            when (status) {
                "processed" -> updateData["processed_at"] = timestamp
                "completed" -> updateData["completed_at"] = timestamp
                "paid_out" -> updateData["paid_out_at"] = timestamp
            }

            val updated = supabase.from("transactions")
                .update(updateData) {
                    filter {
                        eq("id", transactionId)
                    }
                }
                .decodeSingle<Transaction>()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}