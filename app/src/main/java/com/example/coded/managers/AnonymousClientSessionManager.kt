package com.herdmat.coded.managers

import com.herdmat.coded.models.AnonymousClient
import java.security.MessageDigest
import java.util.UUID
import android.provider.Settings
import android.content.Context

class AnonymousClientSessionManager private constructor(private val context: Context? = null) {
    companion object {
        @Volatile
        private var instance: AnonymousClientSessionManager? = null

        fun getInstance(context: Context? = null): AnonymousClientSessionManager =
            instance ?: synchronized(this) {
                instance ?: AnonymousClientSessionManager(context).also { instance = it }
            }
    }

    var currentClient: AnonymousClient? = null
        private set

    private fun generateDeviceHash(): String? {
        context?.let {
            val deviceId = Settings.Secure.getString(it.contentResolver, Settings.Secure.ANDROID_ID)
            return MessageDigest.getInstance("SHA-256").digest(deviceId.toByteArray()).joinToString("") { "%02x".format(it) }
        }
        return null
    }

    fun startNewSession(): String {
        val deviceHash = generateDeviceHash()
        val transactionId = UUID.randomUUID().toString()
        currentClient = AnonymousClient(transactionId, deviceHash, 0)
        return transactionId
    }

    fun loadExistingSession(transactionId: String) {
        val deviceHash = generateDeviceHash()
        currentClient = AnonymousClient(transactionId, deviceHash, 0) // TODO: Fetch risk from backend
    }

    fun updateRiskScore(newScore: Int) {
        currentClient = currentClient?.copy(riskScore = newScore)
    }

    fun endSession() {
        currentClient = null
    }
}