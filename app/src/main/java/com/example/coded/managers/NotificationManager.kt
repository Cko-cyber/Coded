package com.example.coded.managers

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationManager(private val context: Context) {
    private val TAG = "NotificationManager"
    private val firestore = FirebaseFirestore.getInstance()

    fun initializeFCM(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM Token: $token")
                CoroutineScope(Dispatchers.IO).launch {
                    saveFCMToken(userId, token)
                }
            } else {
                Log.e(TAG, "FCM token failed", task.exception)
            }
        }

        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "Subscribed to all_users topic")
            }
    }

    private suspend fun saveFCMToken(userId: String, token: String) {
        try {
            firestore.collection("users").document(userId)
                .update("fcm_token", token)
                .await()
            Log.d(TAG, "FCM token saved for user: $userId")
        } catch (e: Exception) {
            firestore.collection("users").document(userId)
                .set(mapOf("fcm_token" to token))
        }
    }

    fun cleanupFCM(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users").document(userId)
                    .update("fcm_token", null)
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up FCM", e)
            }
        }
    }
}
