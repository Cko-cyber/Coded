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
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔔 FCM SERVICE CREATED!")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🆕 NEW FCM TOKEN GENERATED!")
        Log.d(TAG, "   Token length: ${token.length}")
        Log.d(TAG, "   First 50 chars: ${token.take(50)}...")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📨 MESSAGE RECEIVED!")
        Log.d(TAG, "   From: ${remoteMessage.from}")
        Log.d(TAG, "   Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Log notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "📬 Notification:")
            Log.d(TAG, "   Title: ${notification.title}")
            Log.d(TAG, "   Body: ${notification.body}")
        }

        // Log data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "📦 Data Payload: ${remoteMessage.data}")
        }

        // Handle the message
        handleMessage(remoteMessage)
    }

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

    private fun handleMessage(remoteMessage: RemoteMessage) {
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

        Log.d(TAG, "🔔 Showing notification:")
        Log.d(TAG, "   Title: $title")
        Log.d(TAG, "   Body: $body")
        Log.d(TAG, "   Type: $type")

        showNotification(title, body, type, listingId, conversationId, senderName)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        listingId: String?,
        conversationId: String?,
        senderName: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

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

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // ✅ Using android default icon as fallback
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // ✅ ANDROID DEFAULT
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

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "✅ Notification shown with ID: $notificationId")
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Log.w(TAG, "⚠️ No user logged in, cannot save FCM token")
            return
        }

        Log.d(TAG, "💾 Saving token to Firestore for user: $userId")

        firestore.collection("users")
            .document(userId)
            .update("fcm_token", token)
            .addOnSuccessListener {
                Log.d(TAG, "✅ FCM token saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Update failed: ${e.message}")

                // Fallback: set document
                val userData = hashMapOf(
                    "fcm_token" to token
                )
                firestore.collection("users")
                    .document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ FCM token set via create")
                    }
                    .addOnFailureListener { e2 ->
                        Log.e(TAG, "❌ SET also failed: ${e2.message}")
                    }
            }
    }
}