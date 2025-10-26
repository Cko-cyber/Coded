package com.example.coded

import android.app.Application
import com.google.firebase.FirebaseApp

class CodedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase should auto-initialize with google-services.json
        // But we'll ensure it's initialized
        try {
            FirebaseApp.initializeApp(this)
            println("✅ Firebase initialized successfully")
        } catch (e: Exception) {
            println("❌ Firebase initialization failed: ${e.message}")
        }
    }
}