package com.example.coded.managers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotificationService {
    private val TAG = "NotificationService"
    private val client = OkHttpClient()

    // ✅ Your Supabase credentials
    private val SUPABASE_URL = "https://vxetgoaowehxxifdbdmm.supabase.co"
    private val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4ZXRnb2Fvd2VoeHhpZmRiZG1tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MjE4MzYsImV4cCI6MjA3NjQ5NzgzNn0.n7V7iNzNkMjzb2aNq_Z2ANoC7EhmdQcFB4H3tRBnNVM"

    suspend fun sendNotification(
        recipientId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            Log.d(TAG, "📤 Attempting to send notification to: $recipientId")
            Log.d(TAG, "   Title: $title")
            Log.d(TAG, "   Body: $body")
            Log.d(TAG, "   Data: $data")

            val json = JSONObject().apply {
                put("recipient_id", recipientId)
                put("title", title)
                put("body", body)
                if (data.isNotEmpty()) {
                    put("data", JSONObject(data))
                }
            }

            Log.d(TAG, "📦 Request JSON: ${json.toString()}")

            // ✅ FIXED: Add authorization headers
            val request = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/sendNotification")
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            Log.d(TAG, "🌐 Sending request to: ${request.url}")

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val isSuccessful = response.isSuccessful

            if (isSuccessful) {
                Log.d(TAG, "✅ Notification sent successfully!")
                Log.d(TAG, "   Response code: ${response.code}")
                Log.d(TAG, "   Response body: $responseBody")
            } else {
                Log.e(TAG, "❌ Failed to send notification")
                Log.e(TAG, "   Response code: ${response.code}")
                Log.e(TAG, "   Response body: $responseBody")
            }

            response.close()
            isSuccessful

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception sending notification", e)
            Log.e(TAG, "   Message: ${e.message}")
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
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
        Log.d(TAG, "💬 Preparing message notification for $recipientId from $senderName")

        CoroutineScope(Dispatchers.IO).launch {
            val data = mutableMapOf(
                "type" to "new_message",
                "conversationId" to conversationId,
                "senderName" to senderName
            )

            senderId?.let { data["senderId"] = it }

            val success = sendNotification(
                recipientId = recipientId,
                title = "New message from $senderName",
                body = message,
                data = data
            )

            if (success) {
                Log.d(TAG, "✅ Message notification sent successfully")
            } else {
                Log.e(TAG, "❌ Message notification failed")
            }
        }
    }

    // Other helper methods remain the same...
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