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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.coded.data.AuthRepository
import com.example.coded.managers.NotificationManager
import com.example.coded.managers.NotificationService
import com.example.coded.navigation.NavGraph
import com.example.coded.ui.theme.CodedTheme
import com.example.coded.utils.NotificationPermissionHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            initializeFCM()
        } else {
            Log.w(TAG, "⚠️ Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🚀 MainActivity onCreate()")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // ✅ STEP 1: Check Google Play Services
        checkPlayServices()

        // Initialize notification managers
        notificationManager = NotificationManager(this)
        notificationService = NotificationService()

        // ✅ STEP 2: Request notification permission
        requestNotificationPermission()

        // ✅ STEP 3: Force FCM token retrieval after delay
        lifecycleScope.launch {
            delay(3000) // Wait 3 seconds for Firebase to initialize

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Log.d(TAG, "🔄 Force-triggering FCM token retrieval")
                forceFCMTokenRetrieval(currentUser.uid)
            } else {
                Log.w(TAG, "⚠️ No user logged in yet, will retry on resume")
            }
        }

        // Handle notification intents
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

    /**
     * ✅ CHECK IF GOOGLE PLAY SERVICES IS AVAILABLE
     */
    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

        return when {
            resultCode == ConnectionResult.SUCCESS -> {
                Log.d(TAG, "✅ Google Play Services is AVAILABLE")
                true
            }
            apiAvailability.isUserResolvableError(resultCode) -> {
                Log.e(TAG, "❌ Google Play Services error (resolvable): $resultCode")
                apiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
                false
            }
            else -> {
                Log.e(TAG, "❌ Google Play Services NOT AVAILABLE (not resolvable)")
                Log.e(TAG, "   FCM WILL NOT WORK! Use a device/emulator with Play Services")
                false
            }
        }
    }

    /**
     * ✅ FORCE FCM TOKEN RETRIEVAL WITH COMPREHENSIVE LOGGING
     */
    private fun forceFCMTokenRetrieval(userId: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔑 FORCING FCM TOKEN RETRIEVAL")
        Log.d(TAG, "   User ID: $userId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // ✅ Method 1: Using addOnCompleteListener
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                Log.d(TAG, "📋 FCM Token Task Completed")
                Log.d(TAG, "   Is Successful: ${task.isSuccessful}")
                Log.d(TAG, "   Is Complete: ${task.isComplete}")
                Log.d(TAG, "   Is Canceled: ${task.isCanceled}")

                when {
                    task.isSuccessful -> {
                        val token = task.result
                        Log.d(TAG, "✅ ✅ ✅ TOKEN RETRIEVED!")
                        Log.d(TAG, "   Token is null: ${token == null}")
                        Log.d(TAG, "   Token length: ${token?.length ?: 0}")

                        if (token.isNullOrEmpty()) {
                            Log.e(TAG, "❌ ❌ ❌ TOKEN IS NULL OR EMPTY!")
                        } else {
                            Log.d(TAG, "   First 50 chars: ${token.take(50)}...")
                            Log.d(TAG, "   Last 30 chars: ...${token.takeLast(30)}")

                            // ✅ SAVE IT!
                            saveFCMTokenToFirestore(userId, token)
                        }
                    }
                    task.isCanceled -> {
                        Log.e(TAG, "❌ Token retrieval was CANCELED")
                    }
                    else -> {
                        val exception = task.exception
                        Log.e(TAG, "❌ ❌ ❌ TOKEN RETRIEVAL FAILED!")
                        Log.e(TAG, "   Exception type: ${exception?.javaClass?.simpleName}")
                        Log.e(TAG, "   Message: ${exception?.message}")
                        Log.e(TAG, "   Cause: ${exception?.cause}")
                        exception?.printStackTrace()
                    }
                }
            }
            .addOnSuccessListener { token ->
                Log.d(TAG, "🎯 Success listener triggered!")
                Log.d(TAG, "   Token: ${token.take(50)}...")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Failure listener triggered!")
                Log.e(TAG, "   ${exception.message}")
                exception.printStackTrace()
            }

        // ✅ Method 2: Using coroutines as backup
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Trying coroutine method...")
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "✅ Coroutine method SUCCESS: ${token.take(50)}...")
                saveFCMTokenToFirestore(userId, token)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Coroutine method failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        when (intent.getStringExtra("open_screen")) {
            "messages" -> {
                val conversationId = intent.getStringExtra("conversation_id")
                val senderName = intent.getStringExtra("sender_name")
                Log.d(TAG, "📱 Opening messages: $conversationId from $senderName")
            }
            "notifications" -> {
                val listingId = intent.getStringExtra("listing_id")
                Log.d(TAG, "📱 Opening notifications for listing: $listingId")
            }
            else -> {
                Log.d(TAG, "📱 Opening default screen")
            }
        }
    }

    private fun requestNotificationPermission() {
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            Log.d(TAG, "📱 Requesting notification permission...")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (NotificationPermissionHelper.shouldShowPermissionRationale(this)) {
                    Log.d(TAG, "ℹ️ Showing permission rationale")
                }
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Log.d(TAG, "✅ Notification permission already granted")
            initializeFCM()
        }
    }

    private fun initializeFCM() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            Log.d(TAG, "🔑 Initializing FCM for user: $userId")
            getAndSaveFCMToken(userId)
            notificationManager.initializeFCM(userId)
        } else {
            Log.w(TAG, "⚠️ No user logged in, FCM initialization deferred")
        }
    }

    private fun getAndSaveFCMToken(userId: String) {
        Log.d(TAG, "🟡 Starting FCM token retrieval for user: $userId")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "🔥 FCM Token retrieved: ${token?.length} characters")
                Log.d(TAG, "🔥 First 50 chars: ${token?.take(50)}...")
                saveFCMTokenToFirestore(userId, token)
            } else {
                val exception = task.exception
                Log.e(TAG, "❌ Failed to get FCM token", exception)
                Log.e(TAG, "❌ Error: ${exception?.message}")
            }
        }
    }

    /**
     * ✅ SAVE FCM TOKEN TO FIRESTORE WITH VERIFICATION
     */
    private fun saveFCMTokenToFirestore(userId: String, token: String) {
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "💾 SAVING FCM TOKEN TO FIRESTORE")
        Log.d(TAG, "   User ID: $userId")
        Log.d(TAG, "   Token: ${token.take(50)}...")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "📄 User document EXISTS")
                    Log.d(TAG, "   Current fields: ${document.data?.keys}")

                    val updateData = hashMapOf<String, Any>(
                        "fcm_token" to token,
                        "updated_at" to com.google.firebase.Timestamp.now()
                    )

                    userRef.update(updateData)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ ✅ ✅ TOKEN SAVED SUCCESSFULLY!")
                            Log.d(TAG, "   Token: ${token.take(20)}...")
                            verifyFCMTokenSave(userId)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Update failed: ${e.message}")

                            // Fallback: set with merge
                            val userData = document.data?.toMutableMap() ?: mutableMapOf()
                            userData["fcm_token"] = token
                            userData["updated_at"] = com.google.firebase.Timestamp.now()

                            userRef.set(userData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "✅ Token saved via SET")
                                    verifyFCMTokenSave(userId)
                                }
                                .addOnFailureListener { e2 ->
                                    Log.e(TAG, "❌ SET also failed: ${e2.message}")
                                }
                        }
                } else {
                    Log.w(TAG, "⚠️ User document DOES NOT exist for: $userId")
                    Log.w(TAG, "   Creating new document...")

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
                            Log.e(TAG, "❌ Failed to create document: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to check document existence: ${e.message}")
            }
    }

    /**
     * ✅ VERIFY TOKEN WAS SAVED - WITH DELAY
     */
    private fun verifyFCMTokenSave(userId: String) {
        lifecycleScope.launch {
            try {
                delay(2000) // Wait 2 seconds before verifying

                val userRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)

                val doc = userRef.get().await()

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔍 VERIFICATION RESULTS")

                if (doc.exists()) {
                    val savedToken = doc.getString("fcm_token")

                    Log.d(TAG, "   Document exists: YES")
                    Log.d(TAG, "   Token field: ${if (savedToken != null) "EXISTS" else "NULL"}")

                    if (!savedToken.isNullOrEmpty()) {
                        Log.d(TAG, "   Token length: ${savedToken.length}")
                        Log.d(TAG, "   First 30 chars: ${savedToken.take(30)}...")
                        Log.d(TAG, "✅ ✅ ✅ TOKEN VERIFIED IN FIRESTORE!")
                    } else {
                        Log.e(TAG, "❌ ❌ ❌ TOKEN IS NULL OR EMPTY IN FIRESTORE!")
                        Log.e(TAG, "   Available fields: ${doc.data?.keys}")
                    }
                } else {
                    Log.e(TAG, "❌ User document not found during verification")
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Verification failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

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
        Log.d(TAG, "🔄 onResume() - Refreshing FCM token")

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            forceFCMTokenRetrieval(user.uid)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 onDestroy()")
    }

    fun getNotificationService(): NotificationService {
        return notificationService
    }
}