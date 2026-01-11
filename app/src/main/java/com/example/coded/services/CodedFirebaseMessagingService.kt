package com.example.coded.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.coded.CodedApplication
import com.example.coded.MainActivity
import com.example.coded.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CodedFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM Token: $token")

        // TODO: Send token to your server
        // For now, just log it
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📨 Message received from: ${message.from}")

        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "📦 Data payload: ${message.data}")
            handleDataPayload(message.data)
        }

        // Check if message contains notification payload
        message.notification?.let {
            Log.d(TAG, "🔔 Notification: ${it.title} - ${it.body}")
            sendNotification(
                title = it.title ?: "Oasis",
                body = it.body ?: "",
                data = message.data
            )
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val notificationType = data["type"]

        when (notificationType) {
            "job_accepted" -> {
                sendNotification(
                    title = "Job Accepted! 🎉",
                    body = data["message"] ?: "A provider has accepted your job",
                    data = data,
                    channelId = "job_updates"
                )
            }
            "job_assigned" -> {
                sendNotification(
                    title = "New Job Assigned! 📋",
                    body = data["message"] ?: "You have been assigned a new job",
                    data = data,
                    channelId = "job_updates"
                )
            }
            "job_completed" -> {
                sendNotification(
                    title = "Job Completed! ✅",
                    body = data["message"] ?: "The job has been marked as complete",
                    data = data,
                    channelId = "job_updates"
                )
            }
            "payment_received" -> {
                sendNotification(
                    title = "Payment Received! 💰",
                    body = data["message"] ?: "You have received a payment",
                    data = data,
                    channelId = "payments"
                )
            }
            "new_message" -> {
                sendNotification(
                    title = data["senderName"] ?: "New Message",
                    body = data["message"] ?: "You have a new message",
                    data = data,
                    channelId = "messages"
                )
            }
            else -> {
                sendNotification(
                    title = data["title"] ?: "Oasis",
                    body = data["body"] ?: data["message"] ?: "You have a new notification",
                    data = data
                )
            }
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String = CodedApplication.NOTIFICATION_CHANNEL_ID
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // Add extras based on notification type
            data["type"]?.let { putExtra("notification_type", it) }
            data["jobId"]?.let { putExtra("job_id", it) }
            data["conversationId"]?.let { putExtra("conversation_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        // Add action icon based on type
        val iconResource = when (data["type"]) {
            "job_accepted" -> R.drawable.ic_job_accepted
            "job_assigned" -> R.drawable.ic_job_assigned
            "payment_received" -> R.drawable.ic_payment_received
            "new_message" -> R.drawable.ic_message_notification
            else -> R.drawable.ic_notification
        }
        notificationBuilder.setSmallIcon(iconResource)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "✅ Notification displayed: $title")
    }
}