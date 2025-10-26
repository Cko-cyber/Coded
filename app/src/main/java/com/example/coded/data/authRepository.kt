package com.example.coded.data

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val userRepository = UserRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    init {
        // Load current user on initialization
        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser != null) {
            // Use CoroutineScope with proper imports
            CoroutineScope(Dispatchers.IO).launch {
                loadUserData(currentFirebaseUser.uid)
            }
        }
    }

    private suspend fun loadUserData(userId: String) {
        try {
            val user = userRepository.getUser(userId)
            _currentUser.value = user
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error loading user data", e)
        }
    }

    suspend fun signUp(
        mobileNumber: String,
        fullName: String,
        email: String,
        password: String
    ): AuthResult {
        return try {
            // Check for duplicate user
            val duplicateCheck = checkDuplicateUser(email, mobileNumber)
            if (duplicateCheck.isDuplicate) {
                return AuthResult.Error(duplicateCheck.message)
            }

            // Create Firebase auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return AuthResult.Error("User creation failed")

            // Update profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create Firestore user document
            val user = User(
                id = firebaseUser.uid,
                mobile_number = mobileNumber,
                full_name = fullName,
                email = email,
                token_balance = 5,
                free_listings_used = 0
            )

            val success = userRepository.createUser(user)
            if (success) {
                _currentUser.value = user
                AuthResult.Success(user)
            } else {
                // Rollback: delete Firebase auth user
                firebaseUser.delete().await()
                AuthResult.Error("Failed to create user profile")
            }

        } catch (e: FirebaseAuthUserCollisionException) {
            AuthResult.Error("Email already in use")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign-up failed", e)
            AuthResult.Error(e.message ?: "Unknown error during sign up")
        }
    }

    private data class DuplicateCheckResult(val isDuplicate: Boolean, val message: String = "")

    private suspend fun checkDuplicateUser(email: String, phone: String): DuplicateCheckResult {
        return try {
            val emailQuery = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()

            val phoneQuery = firestore.collection("users")
                .whereEqualTo("mobile_number", phone)
                .get()
                .await()

            when {
                !emailQuery.isEmpty && !phoneQuery.isEmpty ->
                    DuplicateCheckResult(true, "Email and phone number already in use")
                !emailQuery.isEmpty ->
                    DuplicateCheckResult(true, "Email already in use")
                !phoneQuery.isEmpty ->
                    DuplicateCheckResult(true, "Phone number already in use")
                else -> DuplicateCheckResult(false)
            }
        } catch (e: Exception) {
            DuplicateCheckResult(false) // Allow signup if check fails
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return AuthResult.Error("Sign in failed")

            val user = userRepository.getUser(firebaseUser.uid)
            if (user != null) {
                _currentUser.value = user
                AuthResult.Success(user)
            } else {
                AuthResult.Error("User profile not found")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign-in failed", e)
            AuthResult.Error(e.message ?: "Unknown error during sign in")
        }
    }

    suspend fun sendPhoneVerification(phoneNumber: String, activity: Activity): AuthResult {
        return try {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-sign in if verification completes automatically
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("AuthRepository", "Phone verification failed: ${e.message}")
                }
                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    resendToken = token
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)

            AuthResult.Success(User(id = "pending"))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send verification code")
        }
    }

    // Resend phone verification
    suspend fun resendPhoneVerification(phoneNumber: String, activity: Activity): AuthResult {
        return try {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {}
                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("AuthRepository", "Phone verification failed: ${e.message}")
                }
                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    resendToken = token
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .apply {
                    resendToken?.let { setForceResendingToken(it) }
                }
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)

            AuthResult.Success(User(id = "pending"))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to resend verification code")
        }
    }

    suspend fun verifyPhoneOTP(code: String): AuthResult {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId ?: "", code)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return AuthResult.Error("Verification failed")

            var user = userRepository.getUser(firebaseUser.uid)
            if (user == null) {
                user = User(
                    id = firebaseUser.uid,
                    mobile_number = firebaseUser.phoneNumber ?: "",
                    full_name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    token_balance = 5,
                    free_listings_used = 0
                )
                userRepository.createUser(user)
            }

            _currentUser.value = user
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Invalid verification code")
        }
    }

    suspend fun updateUser(user: User): Boolean {
        return try {
            val success = userRepository.updateUser(user)
            if (success) _currentUser.value = user
            success
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update user failed", e)
            false
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        verificationId = null
        resendToken = null
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    suspend fun refreshUserData() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            loadUserData(firebaseUser.uid)
        }
    }
}

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}