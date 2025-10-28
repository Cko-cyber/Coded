package com.example.coded.data

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

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

class AuthRepository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val TAG = "AuthRepository"
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState

    // Add current user state flow
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Store the verification ID and resend token
    private var storedVerificationId: String? = null
    private var storedResendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Load user data from Firestore when user is authenticated
                loadUserData(user.uid)
            } else {
                _currentUser.value = null
            }
        }
    }

    // ADDED: Missing isUserLoggedIn method
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Add this method to get current Firebase user
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Load user data from Firestore
    private fun loadUserData(userId: String) {
        // You can use a coroutine scope here if needed, but for simplicity using callbacks
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    _currentUser.value = user
                } else {
                    // Create a basic user object if document doesn't exist
                    val firebaseUser = auth.currentUser
                    _currentUser.value = User(
                        id = userId,
                        mobile_number = firebaseUser?.phoneNumber ?: "",
                        full_name = firebaseUser?.displayName ?: "",
                        email = firebaseUser?.email ?: "",
                        location = "",
                        profile_pic = "",
                        token_balance = 5,
                        free_listings_used = 0
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user data: ${e.message}")
                // Create a basic user object on failure
                val firebaseUser = auth.currentUser
                _currentUser.value = User(
                    id = userId,
                    mobile_number = firebaseUser?.phoneNumber ?: "",
                    full_name = firebaseUser?.displayName ?: "",
                    email = firebaseUser?.email ?: "",
                    location = "",
                    profile_pic = "",
                    token_balance = 5,
                    free_listings_used = 0
                )
            }
    }

    // Add the missing updateUser method
    suspend fun updateUser(user: User): Boolean {
        return try {
            db.collection("users").document(user.id).set(user.toMap()).await()
            // Update the local state as well
            _currentUser.value = user
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}")
            false
        }
    }

    // Add method to update token balance specifically
    suspend fun updateTokenBalance(userId: String, newBalance: Int): Boolean {
        return try {
            db.collection("users").document(userId).update(
                mapOf(
                    "token_balance" to newBalance,
                    "updated_at" to com.google.firebase.Timestamp.now()
                )
            ).await()

            // Update local state
            val currentUser = _currentUser.value
            if (currentUser != null) {
                _currentUser.value = currentUser.copy(token_balance = newBalance)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating token balance: ${e.message}")
            false
        }
    }

    // Add method to add tokens (for purchases)
    suspend fun addTokens(userId: String, tokensToAdd: Int): Boolean {
        return try {
            val currentUser = _currentUser.value
            if (currentUser != null) {
                val newBalance = currentUser.token_balance + tokensToAdd
                updateTokenBalance(userId, newBalance)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding tokens: ${e.message}")
            false
        }
    }

    // UPDATED: Email authentication method with user profile creation
    suspend fun signUp(
        email: String,
        password: String,
        mobile_number: String = "",
        full_name: String = "",
        location: String = ""
    ): AuthResult {
        return try {
            // Create user with email and password
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                // Create user profile in Firestore
                val newUser = User(
                    id = user.uid,
                    mobile_number = mobile_number,
                    full_name = full_name,
                    email = email,
                    location = location,
                    profile_pic = "",
                    token_balance = 5,
                    free_listings_used = 0,
                    created_at = com.google.firebase.Timestamp.now(),
                    updated_at = com.google.firebase.Timestamp.now(),
                    last_active = com.google.firebase.Timestamp.now()
                )

                // Save user to Firestore
                db.collection("users").document(user.uid).set(newUser.toMap()).await()

                // Update local state
                _currentUser.value = newUser

                AuthResult.Success
            } else {
                AuthResult.Error("User creation failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    fun signOut() {
        auth.signOut()
        _verificationState.value = VerificationState.Idle
        _currentUser.value = null
        storedVerificationId = null
        storedResendToken = null
    }

    fun getCurrentUser() = auth.currentUser

    // Phone authentication methods - these are NOT suspend functions because they use callbacks
    fun sendPhoneVerification(phoneNumber: String, activity: Activity) {
        _verificationState.value = VerificationState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "Verification completed automatically")
                    // Use a coroutine to handle the sign-in
                    coroutineScope.launch {
                        signInWithPhoneAuthCredential(credential)
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Verification failed: ${e.message}")
                    _verificationState.value = VerificationState.Error(e.message ?: "Verification failed")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "OTP code sent")
                    storedVerificationId = verificationId
                    storedResendToken = token
                    _verificationState.value = VerificationState.CodeSent(
                        verificationId = verificationId,
                        resendToken = token
                    )
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendPhoneVerification(phoneNumber: String, activity: Activity) {
        _verificationState.value = VerificationState.Loading

        val resendToken = storedResendToken

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "Verification completed automatically")
                    coroutineScope.launch {
                        signInWithPhoneAuthCredential(credential)
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Verification failed: ${e.message}")
                    _verificationState.value = VerificationState.Error(e.message ?: "Verification failed")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "OTP code resent")
                    storedVerificationId = verificationId
                    storedResendToken = token
                    _verificationState.value = VerificationState.CodeSent(
                        verificationId = verificationId,
                        resendToken = token
                    )
                }
            })

        // Only set force resending token if we have one
        resendToken?.let {
            optionsBuilder.setForceResendingToken(it)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
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
            signInWithPhoneAuthCredential(credential)

            // Check if sign-in was successful
            if (auth.currentUser != null) {
                AuthResult.Success
            } else {
                AuthResult.Error("Authentication failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OTP verification failed: ${e.message}")
            _verificationState.value = VerificationState.Error(e.message ?: "OTP verification failed")
            AuthResult.Error(e.message ?: "OTP verification failed")
        }
    }

    private suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        try {
            auth.signInWithCredential(credential).await()
            _verificationState.value = VerificationState.Verified
            Log.d(TAG, "Phone authentication successful")
        } catch (e: Exception) {
            Log.e(TAG, "Phone authentication failed: ${e.message}")
            _verificationState.value = VerificationState.Error(e.message ?: "Authentication failed")
            throw e // Re-throw to handle in calling function
        }
    }

    // Method to manually refresh user data
    fun refreshUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserData(currentUser.uid)
        }
    }

    // ADDED: Get current user ID for convenience
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // ADDED: Check if user is authenticated and has completed profile
    fun hasCompleteProfile(): Boolean {
        val user = _currentUser.value
        return user?.full_name?.isNotBlank() == true && user?.email?.isNotBlank() == true
    }
}