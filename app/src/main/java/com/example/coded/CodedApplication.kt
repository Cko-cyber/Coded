package com.example.coded

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

class CodedApplication : Application() {

    companion object {
        private const val TAG = "CodedApplication"
        const val NOTIFICATION_CHANNEL_ID = "oasis_notifications"

        // Supabase client - accessible throughout the app
        lateinit var supabase: io.github.jan.supabase.SupabaseClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Initializing Oasis Application...")

        // Initialize Firebase
        initializeFirebase()

        // Initialize Supabase
        initializeSupabase()

        // Create notification channels
        createNotificationChannels()

        // Subscribe to FCM topics
        subscribeFCMTopics()

        Log.d(TAG, "✅ Application initialized successfully")
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ Firebase initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed", e)
        }
    }

    private fun initializeSupabase() {
        try {
            supabase = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
                install(Realtime)
            }
            Log.d(TAG, "✅ Supabase initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Supabase initialization failed", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Main notifications channel
            val mainChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Oasis Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for job updates, messages, and alerts"
                enableVibration(true)
                enableLights(true)
            }

            // Job updates channel
            val jobChannel = NotificationChannel(
                "job_updates",
                "Job Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates about your service jobs"
            }

            // Messages channel
            val messageChannel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New messages from clients or providers"
            }

            // Payment channel
            val paymentChannel = NotificationChannel(
                "payments",
                "Payments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Payment confirmations and receipts"
            }

            notificationManager.createNotificationChannels(
                listOf(mainChannel, jobChannel, messageChannel, paymentChannel)
            )

            Log.d(TAG, "✅ Notification channels created")
        }
    }

    private fun subscribeFCMTopics() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Subscribed to all_users topic")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to subscribe to all_users topic", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ FCM topic subscription failed", e)
        }
    }
}