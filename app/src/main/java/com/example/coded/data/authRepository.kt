package com.example.coded.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.coded.data.models.UserProfile
import com.example.coded.data.models.UserRole
import com.herdmat.coded.managers.AnonymousClientSessionManager
import com.herdmat.coded.models.AnonymousClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

sealed class AuthMode {
    object Idle : AuthMode()
    object Loading : AuthMode()
    data class Authenticated(
        val userProfile: UserProfile,
        val firebaseUser: FirebaseUser?
    ) : AuthMode()
    data class Anonymous(val client: AnonymousClient) : AuthMode()
    data class Guest(val sessionId: String) : AuthMode()
    data class Error(val message: String) : AuthMode()
}

class HerdmatAuthRepository(private val context: Context) {
    private val TAG = "HerdmatAuthRepository"
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Track authentication state
    private val _authState = MutableStateFlow<AuthMode>(AuthMode.Idle)
    val authState: StateFlow<AuthMode> = _authState

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState

    // Session managers
    private val clientSessionManager = AnonymousClientSessionManager.getInstance(context)
    private var storedVerificationId: String? = null
    private var storedResendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Collections
    private val usersRef = db.collection("users")
    private val providersRef = db.collection("providers")
    private val adminsRef = db.collection("admins")

    init {
        // Listen for Firebase auth changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser != null) {
                // Firebase authenticated - load user profile
                coroutineScope.launch {
                    loadUserProfile(firebaseUser.uid)
                }
            } else {
                // No Firebase user - could be anonymous or guest
                checkForAnonymousSession()
            }
        }
    }

    // ==================== ADMIN AUTHENTICATION ====================

    suspend fun adminLogin(email: String, password: String): AuthResult {
        return try {
            _authState.value = AuthMode.Loading

            // Sign in with Firebase
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                // Check if user has admin role
                val profile = getUserProfile(user.uid).await()

                if (profile?.role == UserRole.ADMIN) {
                    _authState.value = AuthMode.Authenticated(profile, user)
                    AuthResult.Success
                } else {
                    auth.signOut()
                    _authState.value = AuthMode.Error("Not authorized as administrator")
                    AuthResult.Error("Not authorized as administrator")
                }
            } else {
                _authState.value = AuthMode.Error("Admin login failed")
                AuthResult.Error("Admin login failed")
            }
        } catch (e: Exception) {
            _authState.value = AuthMode.Error(e.message ?: "Admin login failed")
            AuthResult.Error(e.message ?: "Admin login failed")
        }
    }

    suspend fun createAdminAccount(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        adminLevel: Int = 1,
        permissions: List<String> = emptyList()
    ): AuthResult {
        return try {
            // Verify super admin exists (for security)
            val superAdmin = usersRef
                .whereEqualTo("role", UserRole.ADMIN.name)
                .whereEqualTo("admin_info.admin_level", 3) // SuperAdmin
                .limit(1)
                .get()
                .await()

            if (superAdmin.isEmpty) {
                return AuthResult.Error("No super admin exists to create new admin")
            }

            // Create Firebase user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return AuthResult.Error("User creation failed")

            // Create admin profile
            val adminProfile = UserProfile(
                id = user.uid,
                firebaseUid = user.uid,
                role = UserRole.ADMIN,
                email = email,
                phone = phone,
                fullName = fullName,
                isVerified = true,
                adminInfo = UserProfile.AdminInfo(
                    adminLevel = adminLevel,
                    permissions = permissions
                )
            )

            // Save to Firestore
            usersRef.document(user.uid).set(adminProfile.toMap()).await()
            adminsRef.document(user.uid).set(adminProfile.toMap()).await()

            // Update auth state
            _authState.value = AuthMode.Authenticated(adminProfile, user)

            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to create admin account")
        }
    }

    // ==================== PROVIDER AUTHENTICATION ====================

    suspend fun providerSignUp(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        location: String,
        skills: List<String>,
        hourlyRate: Double? = null
    ): AuthResult {
        return try {
            _authState.value = AuthMode.Loading

            // Create Firebase user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return AuthResult.Error("Provider creation failed")

            // Create provider profile (unverified initially)
            val providerProfile = UserProfile(
                id = user.uid,
                firebaseUid = user.uid,
                role = UserRole.PROVIDER,
                email = email,
                phone = phone,
                fullName = fullName,
                location = location,
                isVerified = false, // Requires admin verification
                providerInfo = UserProfile.ProviderInfo(
                    skills = skills,
                    hourlyRate = hourlyRate,
                    verificationStatus = "PENDING"
                )
            )

            // Save to collections
            usersRef.document(user.uid).set(providerProfile.toMap()).await()
            providersRef.document(user.uid).set(providerProfile.toMap()).await()

            // Update auth state
            _authState.value = AuthMode.Authenticated(providerProfile, user)

            AuthResult.Success
        } catch (e: Exception) {
            _authState.value = AuthMode.Error(e.message ?: "Provider signup failed")
            AuthResult.Error(e.message ?: "Provider signup failed")
        }
    }

    suspend fun providerLogin(email: String, password: String): AuthResult {
        return try {
            _authState.value = AuthMode.Loading

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                val profile = getUserProfile(user.uid).await()

                if (profile?.role == UserRole.PROVIDER) {
                    if (profile.isSuspended) {
                        auth.signOut()
                        return AuthResult.Error("Account suspended. Contact support.")
                    }

                    _authState.value = AuthMode.Authenticated(profile, user)
                    AuthResult.Success
                } else {
                    auth.signOut()
                    AuthResult.Error("Not a provider account")
                }
            } else {
                AuthResult.Error("Provider login failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Provider login failed")
        }
    }

    // ==================== CLIENT AUTHENTICATION ====================

    suspend fun clientSignUp(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        location: String
    ): AuthResult {
        return try {
            _authState.value = AuthMode.Loading

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return AuthResult.Error("Client creation failed")

            val clientProfile = UserProfile(
                id = user.uid,
                firebaseUid = user.uid,
                role = UserRole.CLIENT,
                email = email,
                phone = phone,
                fullName = fullName,
                location = location,
                isVerified = true, // Clients auto-verified
                clientInfo = UserProfile.ClientInfo(
                    preferredPaymentMethod = "MoMo"
                )
            )

            usersRef.document(user.uid).set(clientProfile.toMap()).await()
            _authState.value = AuthMode.Authenticated(clientProfile, user)

            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Client signup failed")
        }
    }

    suspend fun clientLogin(email: String, password: String): AuthResult {
        return try {
            _authState.value = AuthMode.Loading

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                val profile = getUserProfile(user.uid).await()

                if (profile?.role == UserRole.CLIENT) {
                    _authState.value = AuthMode.Authenticated(profile, user)
                    AuthResult.Success
                } else {
                    auth.signOut()
                    AuthResult.Error("Not a client account")
                }
            } else {
                AuthResult.Error("Client login failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Client login failed")
        }
    }

    // ==================== ANONYMOUS CLIENT SESSIONS ====================

    fun startAnonymousSession(): String {
        val transactionId = clientSessionManager.startNewSession()
        val client = clientSessionManager.currentClient ?: return ""

        _authState.value = AuthMode.Anonymous(client)
        Log.d(TAG, "Started anonymous session: $transactionId")

        return transactionId
    }

    fun resumeAnonymousSession(transactionId: String) {
        clientSessionManager.loadExistingSession(transactionId)
        val client = clientSessionManager.currentClient

        if (client != null) {
            _authState.value = AuthMode.Anonymous(client)
            Log.d(TAG, "Resumed anonymous session: $transactionId")
        }
    }

    fun upgradeAnonymousToClient(
        email: String,
        password: String,
        fullName: String,
        phone: String
    ) {
        coroutineScope.launch {
            val currentAnonymous = (_authState.value as? AuthMode.Anonymous)?.client
            if (currentAnonymous == null) {
                _authState.value = AuthMode.Error("No anonymous session to upgrade")
                return@launch
            }

            val result = clientSignUp(email, password, fullName, phone, "")

            if (result is AuthResult.Success) {
                // Link anonymous transaction history to new account
                linkAnonymousHistory(currentAnonymous.transactionId, auth.currentUser?.uid ?: "")
                clientSessionManager.endSession()
            }
        }
    }

    // ==================== GUEST MODE ====================

    fun enterGuestMode() {
        val sessionId = "guest_${System.currentTimeMillis()}"
        _authState.value = AuthMode.Guest(sessionId)
        Log.d(TAG, "Entered guest mode: $sessionId")
    }

    // ==================== PHONE VERIFICATION ====================

    fun sendPhoneVerification(phoneNumber: String, activity: android.app.Activity) {
        _verificationState.value = VerificationState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "Phone verification completed automatically")
                    coroutineScope.launch {
                        signInWithPhoneCredential(credential)
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Phone verification failed: ${e.message}")
                    _verificationState.value = VerificationState.Error(e.message ?: "Verification failed")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "OTP code sent")
                    storedVerificationId = verificationId
                    storedResendToken = token
                    _verificationState.value = VerificationState.CodeSent(verificationId, token)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyPhoneOTP(code: String): AuthResult {
        return try {
            _verificationState.value = VerificationState.Loading

            val verificationId = storedVerificationId
            if (verificationId == null) {
                _verificationState.value = VerificationState.Error("No verification in progress")
                return AuthResult.Error("No verification in progress")
            }

            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneCredential(credential)

            if (auth.currentUser != null) {
                AuthResult.Success
            } else {
                AuthResult.Error("Phone authentication failed")
            }
        } catch (e: Exception) {
            _verificationState.value = VerificationState.Error(e.message ?: "OTP verification failed")
            AuthResult.Error(e.message ?: "OTP verification failed")
        }
    }

    private suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        try {
            auth.signInWithCredential(credential).await()
            _verificationState.value = VerificationState.Verified
        } catch (e: Exception) {
            throw e
        }
    }

    // ==================== UTILITY METHODS ====================

    private suspend fun loadUserProfile(userId: String) {
        try {
            val profile = getUserProfile(userId).await()

            if (profile != null) {
                _authState.value = AuthMode.Authenticated(profile, auth.currentUser)
            } else {
                // Create basic profile if doesn't exist
                val firebaseUser = auth.currentUser
                val basicProfile = UserProfile(
                    id = userId,
                    firebaseUid = userId,
                    role = UserRole.CLIENT, // Default to client
                    email = firebaseUser?.email ?: "",
                    phone = firebaseUser?.phoneNumber ?: "",
                    fullName = firebaseUser?.displayName ?: ""
                )

                usersRef.document(userId).set(basicProfile.toMap()).await()
                _authState.value = AuthMode.Authenticated(basicProfile, firebaseUser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user profile: ${e.message}")
            _authState.value = AuthMode.Error("Failed to load profile")
        }
    }

    private suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = usersRef.document(userId).get().await()
            if (doc.exists()) {
                val data = doc.data ?: return null

                UserProfile(
                    id = data["id"] as? String ?: "",
                    firebaseUid = data["firebase_uid"] as? String ?: "",
                    role = UserRole.fromString(data["role"] as? String ?: "GUEST"),
                    email = data["email"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    fullName = data["full_name"] as? String ?: "",
                    location = data["location"] as? String ?: "",
                    profilePicUrl = data["profile_pic_url"] as? String ?: "",
                    isVerified = data["is_verified"] as? Boolean ?: false,
                    isSuspended = data["is_suspended"] as? Boolean ?: false,
                    trustScore = (data["trust_score"] as? Number)?.toInt() ?: 70,
                    fcmToken = data["fcm_token"] as? String ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun checkForAnonymousSession() {
        val existingClient = clientSessionManager.currentClient
        if (existingClient != null) {
            _authState.value = AuthMode.Anonymous(existingClient)
        } else {
            // Default to guest mode
            enterGuestMode()
        }
    }

    private suspend fun linkAnonymousHistory(anonymousId: String, userId: String) {
        try {
            db.collection("anonymous_transactions")
                .whereEqualTo("anonymous_id", anonymousId)
                .get()
                .await()
                .documents
                .forEach { doc ->
                    doc.reference.update("user_id", userId)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error linking anonymous history: ${e.message}")
        }
    }

    fun signOut() {
        auth.signOut()
        clientSessionManager.endSession()
        _authState.value = AuthMode.Idle
        _verificationState.value = VerificationState.Idle
        storedVerificationId = null
        storedResendToken = null
    }

    fun getCurrentUserProfile(): UserProfile? {
        return when (val state = _authState.value) {
            is AuthMode.Authenticated -> state.userProfile
            else -> null
        }
    }

    fun getCurrentRole(): UserRole? {
        return getCurrentUserProfile()?.role
    }

    fun isAdmin(): Boolean = getCurrentRole() == UserRole.ADMIN
    fun isProvider(): Boolean = getCurrentRole() == UserRole.PROVIDER
    fun isClient(): Boolean = getCurrentRole() == UserRole.CLIENT
    fun isAnonymous(): Boolean = _authState.value is AuthMode.Anonymous
    fun isGuest(): Boolean = _authState.value is AuthMode.Guest

    fun getCurrentUserId(): String? {
        return when (val state = _authState.value) {
            is AuthMode.Authenticated -> state.userProfile.id
            is AuthMode.Anonymous -> state.client.transactionId
            is AuthMode.Guest -> state.sessionId
            else -> null
        }
    }
}

// Keep existing sealed classes for compatibility
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState()
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken?
    ) : VerificationState()
    data class Error(val message: String) : VerificationState()
    object Verified : VerificationState()
}