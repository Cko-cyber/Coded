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

        // Notification Channels
        private const val CHANNEL_MESSAGES = "messages_channel"
        private const val CHANNEL_BOOKINGS = "bookings_channel"
        private const val CHANNEL_GENERAL = "general_channel"

        // Deep Link Actions
        const val ACTION_OPEN_MESSAGES = "com.example.coded.OPEN_MESSAGES"
        const val ACTION_OPEN_BOOKING_DETAILS = "com.example.coded.OPEN_BOOKING_DETAILS"
        const val ACTION_OPEN_NOTIFICATIONS = "com.example.coded.OPEN_NOTIFICATIONS"

        // Extra Keys
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_SENDER_NAME = "sender_name"
        const val EXTRA_LISTING_ID = "listing_id"
        const val EXTRA_BOOKING_TYPE = "booking_type"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔔 FCM SERVICE CREATED!")
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🆕 NEW FCM TOKEN: ${token.take(50)}...")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "📨 MESSAGE RECEIVED from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Herdmat"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "You have a new notification"

        val type = remoteMessage.data["type"] ?: "general"

        when (type) {
            "new_message" -> handleMessageNotification(remoteMessage, title, body)
            "call_booking" -> handleCallBookingNotification(remoteMessage, title, body)
            "viewing_booking" -> handleViewingBookingNotification(remoteMessage, title, body)
            "listing_interest" -> handleListingInterestNotification(remoteMessage, title, body)
            else -> handleGeneralNotification(title, body)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Messages",
                    AndroidNotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "New message notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                },
                NotificationChannel(
                    CHANNEL_BOOKINGS,
                    "Bookings & Appointments",
                    AndroidNotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Call and viewing booking notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                },
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    AndroidNotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General app notifications"
                }
            )

            val notificationManager = getSystemService(AndroidNotificationManager::class.java)
            channels.forEach { notificationManager.createNotificationChannel(it) }

            Log.d(TAG, "✅ Created ${channels.size} notification channels")
        }
    }

    // ============================================================
    // MESSAGE NOTIFICATION - Opens Messages Screen
    // ============================================================
    private fun handleMessageNotification(
        remoteMessage: RemoteMessage,
        title: String,
        body: String
    ) {
        val conversationId = remoteMessage.data["conversationId"]
        val senderName = remoteMessage.data["senderName"]
        val senderId = remoteMessage.data["senderId"]

        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_MESSAGES
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra(EXTRA_SENDER_NAME, senderName)
            putExtra("senderId", senderId)
        }

        showNotification(
            channelId = CHANNEL_MESSAGES,
            title = title,
            body = body,
            intent = intent,
            icon = R.drawable.ic_message_notification,
            notificationId = conversationId?.hashCode() ?: System.currentTimeMillis().toInt()
        )
    }

    // ============================================================
    // CALL BOOKING NOTIFICATION - Opens Booking Details Screen
    // ============================================================
    private fun handleCallBookingNotification(
        remoteMessage: RemoteMessage,
        title: String,
        body: String
    ) {
        val listingId = remoteMessage.data["listingId"]
        val buyerName = remoteMessage.data["buyerName"]
        val buyerId = remoteMessage.data["buyerId"]
        val preferredDate = remoteMessage.data["preferredDate"]
        val preferredTime = remoteMessage.data["preferredTime"]

        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_BOOKING_DETAILS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LISTING_ID, listingId)
            putExtra(EXTRA_BOOKING_TYPE, "call")
            putExtra("buyerName", buyerName)
            putExtra("buyerId", buyerId)
            putExtra("preferredDate", preferredDate)
            putExtra("preferredTime", preferredTime)
        }

        showNotification(
            channelId = CHANNEL_BOOKINGS,
            title = title,
            body = body,
            intent = intent,
            icon = R.drawable.ic_book_call_notification,
            notificationId = listingId?.hashCode() ?: System.currentTimeMillis().toInt()
        )
    }

    // ============================================================
    // VIEWING BOOKING NOTIFICATION - Opens Booking Details Screen
    // ============================================================
    private fun handleViewingBookingNotification(
        remoteMessage: RemoteMessage,
        title: String,
        body: String
    ) {
        val listingId = remoteMessage.data["listingId"]
        val buyerName = remoteMessage.data["buyerName"]
        val buyerId = remoteMessage.data["buyerId"]
        val preferredDate = remoteMessage.data["preferredDate"]
        val preferredTime = remoteMessage.data["preferredTime"]
        val numberOfPeople = remoteMessage.data["numberOfPeople"]

        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_BOOKING_DETAILS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LISTING_ID, listingId)
            putExtra(EXTRA_BOOKING_TYPE, "viewing")
            putExtra("buyerName", buyerName)
            putExtra("buyerId", buyerId)
            putExtra("preferredDate", preferredDate)
            putExtra("preferredTime", preferredTime)
            putExtra("numberOfPeople", numberOfPeople)
        }

        showNotification(
            channelId = CHANNEL_BOOKINGS,
            title = title,
            body = body,
            intent = intent,
            icon = R.drawable.ic_schedule_viewing_notification,
            notificationId = listingId?.hashCode() ?: System.currentTimeMillis().toInt()
        )
    }

    // ============================================================
    // LISTING INTEREST NOTIFICATION
    // ============================================================
    private fun handleListingInterestNotification(
        remoteMessage: RemoteMessage,
        title: String,
        body: String
    ) {
        val listingId = remoteMessage.data["listingId"]

        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_NOTIFICATIONS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LISTING_ID, listingId)
            putExtra(EXTRA_NOTIFICATION_TYPE, "listing_interest")
        }

        showNotification(
            channelId = CHANNEL_GENERAL,
            title = title,
            body = body,
            intent = intent,
            icon = R.drawable.ic_notification,
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    // ============================================================
    // GENERAL NOTIFICATION
    // ============================================================
    private fun handleGeneralNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        showNotification(
            channelId = CHANNEL_GENERAL,
            title = title,
            body = body,
            intent = intent,
            icon = R.drawable.ic_notification,
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    // ============================================================
    // UNIFIED NOTIFICATION DISPLAY
    // ============================================================
    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        intent: Intent,
        icon: Int,
        notificationId: Int
    ) {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setColor(resources.getColor(R.color.HerdmatGreen, null))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "✅ Notification shown: ID=$notificationId, Channel=$channelId")
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .update("fcm_token", token)
            .addOnSuccessListener {
                Log.d(TAG, "✅ FCM token saved")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to save token: ${e.message}")
            }
    }
}