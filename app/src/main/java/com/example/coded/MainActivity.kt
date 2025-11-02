package com.example.coded

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.coded.data.AuthRepository
import com.example.coded.managers.NotificationManager
import com.example.coded.managers.NotificationService
import com.example.coded.navigation.NavGraph
import com.example.coded.ui.theme.CodedTheme
import com.example.coded.utils.NotificationPermissionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationService: NotificationService

    // Register for notification permission result
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "✅ Notification permission granted")
            // Initialize FCM with current user
            initializeFCM()
        } else {
            Log.w(TAG, "⚠️ Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen for better performance
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Initialize notification managers
        notificationManager = NotificationManager(this)
        notificationService = NotificationService()

        // Set up FCM token refresh listener
        setupFCMTokenRefreshListener()

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Handle notification intents when app is opened from notification
        handleNotificationIntent(intent)

        setContent {
            CodedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authRepository = remember { AuthRepository() }

                    NavGraph(
                        navController = navController,
                        authRepository = authRepository
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle notification intents when app is already running
        intent?.let { handleNotificationIntent(it) }
    }

    /**
     * Handle notification intents when app is opened from notification
     */
    private fun handleNotificationIntent(intent: Intent) {
        when (intent.getStringExtra("open_screen")) {
            "messages" -> {
                val conversationId = intent.getStringExtra("conversation_id")
                val senderName = intent.getStringExtra("sender_name")
                Log.d(TAG, "📱 Opening messages screen for conversation: $conversationId from $senderName")
                // You can navigate to specific conversation here
            }
            "notifications" -> {
                val listingId = intent.getStringExtra("listing_id")
                Log.d(TAG, "📱 Opening notifications screen for listing: $listingId")
                // You can navigate to notifications screen here
            }
            else -> {
                Log.d(TAG, "📱 Opening default screen")
            }
        }
    }

    /**
     * Request notification permission for Android 13+
     */
    private fun requestNotificationPermission() {
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            Log.d(TAG, "📱 Requesting notification permission...")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (NotificationPermissionHelper.shouldShowPermissionRationale(this)) {
                    // Show explanation to user before requesting permission
                    Log.d(TAG, "ℹ️ Showing permission rationale")
                }
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Log.d(TAG, "✅ Notification permission already granted")
            initializeFCM()
        }
    }

    /**
     * Initialize FCM with current user - GET AND STORE FCM TOKEN
     */
    private fun initializeFCM() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            Log.d(TAG, "🔑 Initializing FCM for user: $userId")

            // Get FCM token and save to user profile
            getAndSaveFCMToken(userId)

            notificationManager.initializeFCM(userId)
        } else {
            Log.w(TAG, "⚠️ No user logged in, FCM initialization deferred")
        }
    }

    /**
     * Get FCM token and save it to user's Firestore document
     */
    private fun getAndSaveFCMToken(userId: String) {
        Log.d(TAG, "🟡 Starting FCM token retrieval for user: $userId")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "🔥 FCM Token retrieved successfully: ${token?.length} characters")
                Log.d(TAG, "🔥 First 50 chars: ${token?.take(50)}...")

                // Save token to user profile in Firestore
                saveFCMTokenToFirestore(userId, token)
            } else {
                val exception = task.exception
                Log.e(TAG, "❌ Failed to get FCM token", exception)
                Log.e(TAG, "❌ Error details: ${exception?.message}")
            }
        }

        // Also set up a listener for token refresh
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d(TAG, "🔄 FCM Token (from success listener): ${token.length} characters")
            Log.d(TAG, "🔄 First 50 chars: ${token.take(50)}...")
            saveFCMTokenToFirestore(userId, token)
        }
    }

    /**
     * Save FCM token to user's document in Firestore
     */
    private fun saveFCMTokenToFirestore(userId: String, token: String) {
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)

        Log.d(TAG, "🟡 Attempting to save FCM token for user: $userId")

        // First, let's check if the user document exists and see its current structure
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Log.d(TAG, "📄 User document exists, current fields: ${document.data?.keys}")

                // Try multiple field name variations to ensure it works
                val updateData = hashMapOf<String, Any>(
                    "fcm_token" to token,
                    "updated_at" to com.google.firebase.Timestamp.now()
                )

                userRef.update(updateData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ FCM token successfully saved to user: $userId")
                        Log.d(TAG, "✅ Token: ${token.take(20)}...")

                        // Verify the save by reading it back
                        verifyFCMTokenSave(userId)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to update user document with FCM token", e)
                        Log.e(TAG, "❌ Error: ${e.message}")

                        // Try alternative approach - set with merge
                        val userData = document.data?.toMutableMap() ?: mutableMapOf()
                        userData["fcm_token"] = token
                        userData["updated_at"] = com.google.firebase.Timestamp.now()

                        userRef.set(userData).addOnSuccessListener {
                            Log.d(TAG, "✅ FCM token saved using set() with merge")
                            verifyFCMTokenSave(userId)
                        }.addOnFailureListener { e2 ->
                            Log.e(TAG, "❌ Failed to save with set()", e2)
                        }
                    }
            } else {
                Log.w(TAG, "⚠️ User document does NOT exist for: $userId")
                Log.w(TAG, "⚠️ FCM token cannot be saved until user profile is created")

                // Create a basic user document with FCM token
                val newUserData = hashMapOf(
                    "id" to userId,
                    "fcm_token" to token,
                    "created_at" to com.google.firebase.Timestamp.now(),
                    "updated_at" to com.google.firebase.Timestamp.now()
                )

                userRef.set(newUserData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Created new user document with FCM token")
                        verifyFCMTokenSave(userId)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to create user document", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to check if user document exists", e)
        }
    }

    /**
     * Verify that FCM token was actually saved
     */
    private fun verifyFCMTokenSave(userId: String) {
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val savedToken = document.getString("fcm_token")
                if (savedToken != null) {
                    Log.d(TAG, "✅ VERIFIED: FCM token is saved in user document")
                    Log.d(TAG, "✅ Saved token: ${savedToken.take(20)}...")
                    Log.d(TAG, "✅ Full token length: ${savedToken.length} characters")
                } else {
                    Log.w(TAG, "⚠️ FCM token field exists but is null")
                    Log.w(TAG, "⚠️ Available fields: ${document.data?.keys}")
                }
            } else {
                Log.e(TAG, "❌ User document not found during verification")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to verify FCM token save", e)
        }
    }

    /**
     * Set up FCM token refresh listener
     */
    private fun setupFCMTokenRefreshListener() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "🔄 FCM Token refreshed: ${token?.take(20)}...")
                val currentUser = FirebaseAuth.getInstance().currentUser
                currentUser?.let { user ->
                    saveFCMTokenToFirestore(user.uid, token)
                }
            }
        }
    }

    /**
     * Handle permission request result (for backward compatibility)
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "✅ Notification permission granted via onRequestPermissionsResult")
                    initializeFCM()
                } else {
                    Log.w(TAG, "⚠️ Notification permission denied via onRequestPermissionsResult")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh FCM token when app comes to foreground to ensure it's current
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            Log.d(TAG, "🔄 Refreshing FCM token on resume")
            getAndSaveFCMToken(user.uid)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up FCM when activity is destroyed (optional)
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Only call cleanup if the method exists in NotificationManager
            // notificationManager.cleanupFCM(user.uid) // Commented out if method doesn't exist
        }
    }

    // Public methods to access notification service from other parts of the app
    fun getNotificationService(): NotificationService {
        return notificationService
    }
}