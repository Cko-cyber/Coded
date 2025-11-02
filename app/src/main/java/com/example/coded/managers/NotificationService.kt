package com.example.coded.managers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class NotificationService {
    private val TAG = "NotificationService"
    private val client = OkHttpClient()

    // ⚠️ REPLACE WITH YOUR ACTUAL SUPABASE PROJECT URL
    private val BASE_URL = "https://vxetgoaowehxxifdbdmm.supabase.co/functions/v1/sendNotification"

    suspend fun sendNotification(
        recipientId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            val json = JSONObject().apply {
                put("recipient_id", recipientId)
                put("title", title)
                put("body", body)
                if (data.isNotEmpty()) {
                    put("data", JSONObject(data))
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL/sendNotification")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val isSuccessful = response.isSuccessful

            if (isSuccessful) {
                Log.d(TAG, "✅ Notification sent successfully to: $recipientId - Response: $responseBody")
            } else {
                Log.e(TAG, "❌ Failed to send notification: ${response.code} - $responseBody")
            }

            response.close()
            isSuccessful

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification: ${e.message}")
            false
        }
    }

    // Helper method for message notifications
    fun sendMessageNotification(
        recipientId: String,
        senderName: String,
        message: String,
        conversationId: String,
        senderId: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = mutableMapOf(
                "type" to "new_message",
                "conversationId" to conversationId,
                "senderName" to senderName
            )

            senderId?.let { data["senderId"] = it }

            sendNotification(
                recipientId = recipientId,
                title = "New message from $senderName",
                body = message,
                data = data
            )
        }
    }

    // Helper method for call booking notifications
    fun sendCallBookingNotification(
        recipientId: String,
        buyerName: String,
        listingTitle: String,
        listingId: String,
        buyerId: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = mutableMapOf(
                "type" to "call_booking",
                "listingId" to listingId,
                "buyerName" to buyerName,
                "listingTitle" to listingTitle
            )

            buyerId?.let { data["buyerId"] = it }

            sendNotification(
                recipientId = recipientId,
                title = "Call request from $buyerName",
                body = "Wants to schedule a call about $listingTitle",
                data = data
            )
        }
    }

    // Helper method for general notifications
    fun sendGeneralNotification(
        recipientId: String,
        title: String,
        body: String,
        type: String = "general"
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            sendNotification(
                recipientId = recipientId,
                title = title,
                body = body,
                data = mapOf("type" to type)
            )
        }
    }

    // Helper method for listing interest notifications
    fun sendListingInterestNotification(
        recipientId: String,
        buyerName: String,
        listingTitle: String,
        listingId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            sendNotification(
                recipientId = recipientId,
                title = "New interest in your listing",
                body = "$buyerName is interested in $listingTitle",
                data = mapOf(
                    "type" to "listing_interest",
                    "listingId" to listingId,
                    "buyerName" to buyerName
                )
            )
        }
    }
}