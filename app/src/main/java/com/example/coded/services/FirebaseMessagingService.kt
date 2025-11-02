package com.example.coded.services

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.coded.MainActivity
import com.example.coded.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "herdmat_notifications"
        private const val CHANNEL_NAME = "Herdmat Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for messages and updates"
    }

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔔 FCM Service Created")
        createNotificationChannel()
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM Token: $token")

        // Save token to Firestore
        saveTokenToFirestore(token)
    }

    /**
     * Called when a message is received
     * This handles BOTH foreground and background messages when using data-only payload
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "📨 Message received from: ${remoteMessage.from}")

        // Log notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "📬 Notification Title: ${notification.title}")
            Log.d(TAG, "📬 Notification Body: ${notification.body}")
        }

        // Log data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "📦 Data Payload: ${remoteMessage.data}")
        }

        // Handle the message and show notification
        handleMessage(remoteMessage)
    }

    /**
     * Create notification channel for Android O+
     * MUST be called before showing any notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = AndroidNotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "✅ Notification channel created: $CHANNEL_ID")
        }
    }

    /**
     * Handle incoming message and show notification
     */
    private fun handleMessage(remoteMessage: RemoteMessage) {
        // Extract title and body from either notification or data payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Herdmat"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "You have a new notification"

        val type = remoteMessage.data["type"] ?: "general"
        val listingId = remoteMessage.data["listingId"]
        val conversationId = remoteMessage.data["conversationId"]
        val senderName = remoteMessage.data["senderName"]

        Log.d(TAG, "🔔 Showing notification: $title - $body - Type: $type")

        // Show the notification
        showNotification(title, body, type, listingId, conversationId, senderName)
    }

    /**
     * Display the notification
     */
    private fun showNotification(
        title: String,
        body: String,
        type: String,
        listingId: String?,
        conversationId: String?,
        senderName: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

        // Create intent to open the app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Add extras based on notification type
            when (type) {
                "new_message" -> {
                    putExtra("open_screen", "messages")
                    conversationId?.let { putExtra("conversation_id", it) }
                    senderName?.let { putExtra("sender_name", it) }
                }
                "call_booking", "viewing_booking", "listing_interest" -> {
                    putExtra("open_screen", "notifications")
                    listingId?.let { putExtra("listing_id", it) }
                }
                else -> {
                    putExtra("open_screen", "home")
                }
            }
        }

        // Create pending intent with FLAG_IMMUTABLE for Android 12+
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            pendingIntentFlags
        )

        // Get default notification sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this drawable exists
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Show the notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "✅ Notification shown with ID: $notificationId")
    }

    /**
     * Save FCM token to Firestore
     */
    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .update("fcm_token", token)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ FCM token saved to Firestore for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to save FCM token: ${e.message}")
                    // Try to set the document if update fails
                    val userData = hashMapOf(
                        "fcm_token" to token
                    )
                    firestore.collection("users")
                        .document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ FCM token set via create for user: $userId")
                        }
                        .addOnFailureListener { e2 ->
                            Log.e(TAG, "❌ Failed to set FCM token: ${e2.message}")
                        }
                }
        } else {
            Log.w(TAG, "⚠️ No user logged in, cannot save FCM token")
        }
    }
}