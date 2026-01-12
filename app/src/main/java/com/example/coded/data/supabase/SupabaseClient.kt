package com.example.coded.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.Phone
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import android.content.Context
import com.example.coded.BuildConfig
import kotlinx.datetime.Instant

// ============================================
// SUPABASE CLIENT SETUP
// ============================================

object SupabaseClient {
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

    fun getInstance(): SupabaseClient = client
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

    // Location
    val location: String? = null, // GeoJSON Point
    val region: String? = null,
    val town: String? = null,
    val address: String? = null,

    // Verification
    val is_verified: Boolean = false,
    val verification_status: String = "pending",
    val verified_at: String? = null,

    // Performance
    val rating: Double = 0.0,
    val total_ratings: Int = 0,
    val total_jobs: Int = 0,
    val completed_jobs: Int = 0,
    val cancelled_jobs: Int = 0,
    val acceptance_rate: Double = 0.0,
    val completion_rate: Double = 0.0,
    val average_response_time: Int = 0,

    // Availability
    val is_online: Boolean = false,
    val is_available: Boolean = true,
    val last_location_update: String? = null,

    // Banking
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

    // Client
    val client_id: String? = null,
    val anonymous_client_id: String? = null,
    val client_name: String,
    val client_phone: String,
    val client_email: String? = null,

    // Service
    val service_type: String,
    val service_variant: String? = null,
    val title: String,
    val description: String,

    // Location
    val location_address: String,
    val location: String? = null, // GeoJSON Point
    val region: String? = null,
    val town: String? = null,

    // Specifics
    val estimated_area: Double? = null,
    val vegetation_type: String? = null,
    val growth_stage: String? = null,
    val terrain_type: String? = null,
    val needs_disposal: Boolean = false,
    val is_urgent: Boolean = false,
    val preferred_date: String? = null,

    // Pricing
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

    // Payment
    val payment_method: String = "mobile_money",
    val mobile_money_provider: String? = null,
    val mobile_money_number: String? = null,
    val payment_status: String = "pending",
    val transaction_id: String? = null,
    val payment_reference: String? = null,
    val paid_at: String? = null,

    // Provider
    val provider_id: String? = null,
    val provider_name: String? = null,
    val provider_phone: String? = null,
    val provider_rating: Double? = null,
    val assigned_by: String? = null,
    val assigned_by_name: String? = null,
    val assigned_at: String? = null,
    val assignment_notes: String? = null,
    val deadline: String? = null,

    // Status
    val status: String = "draft",
    val accepted_at: String? = null,
    val started_at: String? = null,
    val completed_at: String? = null,
    val verified_at: String? = null,
    val cancelled_at: String? = null,
    val cancelled_by: String? = null,
    val cancellation_reason: String? = null,

    // Completion
    val completion_notes: String? = null,
    val completion_images: List<String> = emptyList(),

    // Refund
    val refund_amount: Double? = null,
    val refund_status: String? = null,

    // Images
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

@Serializable
data class Conversation(
    val id: String? = null,
    val job_id: String? = null,
    val client_id: String? = null,
    val anonymous_client_id: String? = null,
    val provider_id: String? = null,
    val admin_id: String? = null,
    val last_message: String? = null,
    val last_message_at: String? = null,
    val client_unread_count: Int = 0,
    val provider_unread_count: Int = 0,
    val admin_unread_count: Int = 0,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class Message(
    val id: String? = null,
    val conversation_id: String,
    val sender_id: String,
    val sender_type: String, // client, provider, admin, anonymous
    val sender_name: String,
    val message_text: String,
    val attachments: List<String> = emptyList(),
    val is_read: Boolean = false,
    val read_at: String? = null,
    val created_at: String? = null
)

@Serializable
data class Notification(
    val id: String? = null,
    val user_id: String,
    val title: String,
    val body: String,
    val notification_type: String,
    val job_id: String? = null,
    val conversation_id: String? = null,
    val is_read: Boolean = false,
    val read_at: String? = null,
    val created_at: String? = null,
    val expires_at: String? = null
)

// ============================================
// AUTHENTICATION REPOSITORY
// ============================================

class SupabaseAuthRepository(private val context: Context) {
    private val supabase = SupabaseClient.getInstance()

    // Anonymous client session
    suspend fun createAnonymousSession(
        phoneNumber: String,
        mobileMoneyProvider: String,
        deviceHash: String?
    ): Result<AnonymousClient> {
        return try {
            val sessionToken = generateSessionToken()
            val expiresAt = calculateExpiryDate(72) // 72 hours default

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

    // Provider login with phone
    suspend fun loginProviderWithPhone(
        phoneNumber: String,
        password: String
    ): Result<Provider> {
        return try {
            // Authenticate with Supabase Auth
            supabase.auth.signInWith(Phone) {
                phone = "+268$phoneNumber"
                this.password = password
            }

            // Get provider profile
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

    // Provider login with email
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

    // Admin login
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

    // Change password on first login
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

    // Logout
    suspend fun logout(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get current user
    fun getCurrentUser() = supabase.auth.currentUserOrNull()

    private fun generateSessionToken(): String {
        return "ANON_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun calculateExpiryDate(hours: Int): String {
        val now = System.currentTimeMillis()
        val expiry = now + (hours * 60 * 60 * 1000)
        return Instant.fromEpochMilliseconds(expiry).toString()
    }
}

// ============================================
// JOB REPOSITORY
// ============================================

class SupabaseJobRepository(private val context: Context) {
    private val supabase = SupabaseClient.getInstance()

    // Create job (anonymous or authenticated)
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

    // Update job payment status
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
                    set("paid_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
                    set("status", "pending_assignment")
                }) {
                    filter {
                        eq("id", jobId)
                    }
                }
                .decodeSingle<Job>()

            // Trigger job suggestion generation
            generateJobSuggestions(jobId)

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get suggested jobs for provider (with real-time updates)
    suspend fun getSuggestedJobsForProvider(providerId: String): Result<List<Job>> {
        return try {
            // Get job suggestions
            val suggestions = supabase.from("job_suggestions")
                .select() {
                    filter {
                        eq("provider_id", providerId)
                        eq("is_accepted", false)
                        eq("is_rejected", false)
                    }
                }
                .decodeList<JobSuggestion>()

            // Get full job details
            val jobIds = suggestions.map { it.job_id }
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

    // Listen to real-time job updates
    fun listenToJobUpdates(jobId: String): Flow<Job> {
        return supabase.realtime.channel("job_updates_$jobId")
            .postgresChangeFlow<Job>(schema = "public") {
                table = "jobs"
                filter = "id=eq.$jobId"
            }
    }

    // Provider accepts job
    suspend fun acceptJob(jobId: String, providerId: String): Result<Job> {
        return try {
            val updated = supabase.from("jobs")
                .update({
                    set("status", "accepted")
                    set("accepted_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
                }) {
                    filter {
                        eq("id", jobId)
                        eq("provider_id", providerId)
                    }
                }
                .decodeSingle<Job>()

            // Update job suggestion
            supabase.from("job_suggestions")
                .update({
                    set("is_accepted", true)
                    set("accepted_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
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

    // Complete job
    suspend fun completeJob(
        jobId: String,
        providerId: String,
        completionNotes: String?,
        completionImages: List<String>
    ): Result<Job> {
        return try {
            val updated = supabase.from("jobs")
                .update({
                    set("status", "completed")
                    set("completed_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
                    set("completion_notes", completionNotes)
                    set("completion_images", completionImages)
                }) {
                    filter {
                        eq("id", jobId)
                        eq("provider_id", providerId)
                    }
                }
                .decodeSingle<Job>()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Submit rating (closes ticket and expires anonymous session)
    suspend fun submitRating(
        jobId: String,
        providerId: String,
        rating: Int,
        review: String?,
        qualityRating: Int? = null,
        professionalismRating: Int? = null,
        timelinessRating: Int? = null
    ): Result<Rating> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id

            // Get job to check if anonymous
            val job = supabase.from("jobs")
                .select() {
                    filter {
                        eq("id", jobId)
                    }
                }
                .decodeSingle<Job>()

            val ratingData = Rating(
                job_id = jobId,
                provider_id = providerId,
                client_id = userId,
                anonymous_client_id = job.anonymous_client_id,
                rating = rating,
                review = review,
                quality_rating = qualityRating,
                professionalism_rating = professionalismRating,
                timeliness_rating = timelinessRating
            )

            val created = supabase.from("ratings")
                .insert(ratingData)
                .decodeSingle<Rating>()

            // Update job status to verified
            supabase.from("jobs")
                .update({
                    set("status", "verified")
                    set("verified_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
                }) {
                    filter {
                        eq("id", jobId)
                    }
                }

            // Expire anonymous session if applicable
            job.anonymous_client_id?.let { anonId ->
                supabase.from("anonymous_clients")
                    .update({
                        set("is_expired", true)
                    }) {
                        filter {
                            eq("id", anonId)
                        }
                    }
            }

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Auto-generate job suggestions based on proximity, skill, and performance
    private suspend fun generateJobSuggestions(jobId: String) {
        try {
            // This would typically be done via a Postgres function or Edge Function
            // For now, we'll call a simplified version

            // Get job details
            val job = supabase.from("jobs")
                .select() {
                    filter {
                        eq("id", jobId)
                    }
                }
                .decodeSingle<Job>()

            // Get eligible providers
            val providers = supabase.from("providers")
                .select() {
                    filter {
                        eq("is_verified", true)
                        eq("is_available", true)
                        contains("service_types", listOf(job.service_type))
                    }
                }
                .decodeList<Provider>()

            // Calculate scores and create suggestions
            providers.forEach { provider ->
                val proximityScore = calculateProximityScore(job, provider)
                val skillScore = calculateSkillScore(job, provider)
                val performanceScore = provider.rating * 20 // 0-100 scale

                val totalScore = (proximityScore * 0.4 + skillScore * 0.3 + performanceScore * 0.3)

                if (totalScore >= 50) { // Only suggest if score is decent
                    val suggestion = JobSuggestion(
                        job_id = jobId,
                        provider_id = provider.id,
                        proximity_score = proximityScore,
                        skill_match_score = skillScore,
                        performance_score = performanceScore,
                        total_score = totalScore,
                        expires_at = calculateExpiryDate(24) // 24 hours
                    )

                    supabase.from("job_suggestions")
                        .insert(suggestion)
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the payment
            println("Failed to generate suggestions: ${e.message}")
        }
    }

    private fun calculateProximityScore(job: Job, provider: Provider): Double {
        // Simplified - in production, use PostGIS distance calculation
        return 75.0 // Placeholder
    }

    private fun calculateSkillScore(job: Job, provider: Provider): Double {
        val matchingSkills = provider.service_types.filter { it == job.service_type }
        return if (matchingSkills.isNotEmpty()) 100.0 else 0.0
    }

    private fun calculateExpiryDate(hours: Int): String {
        val now = System.currentTimeMillis()
        val expiry = now + (hours * 60 * 60 * 1000)
        return Instant.fromEpochMilliseconds(expiry).toString()
    }
}

// ============================================
// MESSAGING REPOSITORY
// ============================================

class SupabaseMessagingRepository(private val context: Context) {
    private val supabase = SupabaseClient.getInstance()

    // Send message
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderType: String,
        senderName: String,
        messageText: String,
        attachments: List<String> = emptyList()
    ): Result<Message> {
        return try {
            val message = Message(
                conversation_id = conversationId,
                sender_id = senderId,
                sender_type = senderType,
                sender_name = senderName,
                message_text = messageText,
                attachments = attachments
            )

            val created = supabase.from("messages")
                .insert(message)
                .decodeSingle<Message>()

            // Update conversation
            supabase.from("conversations")
                .update({
                    set("last_message", messageText)
                    set("last_message_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
                }) {
                    filter {
                        eq("id", conversationId)
                    }
                }

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listen to real-time messages
    fun listenToMessages(conversationId: String): Flow<Message> {
        return supabase.realtime.channel("messages_$conversationId")
            .postgresChangeFlow<Message>(schema = "public") {
                table = "messages"
                filter = "conversation_id=eq.$conversationId"
            }
    }
}