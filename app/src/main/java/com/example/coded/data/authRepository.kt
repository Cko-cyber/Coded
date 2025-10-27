package com.example.coded.data

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
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

    // Session timeout: 30 minutes of inactivity
    private val SESSION_TIMEOUT_MS = 30 * 60 * 1000L

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                checkSessionValidity(currentFirebaseUser.uid)
            }
        }
    }

    // Check if session is still valid
    private suspend fun checkSessionValidity(userId: String) {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val lastActive = doc.getTimestamp("last_active")
                val now = System.currentTimeMillis()
                val lastActiveMs = lastActive?.toDate()?.time ?: 0

                if (now - lastActiveMs > SESSION_TIMEOUT_MS) {
                    Log.w("AuthRepository", "Session expired - logging out")
                    signOut()
                } else {
                    loadUserData(userId)
                    updateLastActive(userId)
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking session", e)
        }
    }

    // Update last active timestamp
    suspend fun updateLastActive(userId: String? = null) {
        val uid = userId ?: auth.currentUser?.uid ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .update("last_active", Timestamp.now())
                .await()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error updating last active", e)
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

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

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

            // Validate inputs
            if (fullName.isBlank() || fullName.length < 2) {
                return AuthResult.Error("Please enter a valid full name")
            }

            if (!isValidEmail(email)) {
                return AuthResult.Error("Please enter a valid email address")
            }

            if (password.length < 6) {
                return AuthResult.Error("Password must be at least 6 characters")
            }

            // Create Firebase auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return AuthResult.Error("User creation failed")

            // Update profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create Firestore user document with proper field names
            val user = User(
                id = firebaseUser.uid,
                mobile_number = mobileNumber,
                full_name = fullName,
                email = email,
                location = "",
                profile_pic = "",
                token_balance = 5, // Starting tokens
                free_listings_used = 0,
                free_listings_reset_date = Timestamp.now(),
                created_at = Timestamp.now(),
                updated_at = Timestamp.now(),
                last_active = Timestamp.now()
            )

            // Save to Firestore using the toMap() method
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user.toMap())
                .await()

            _currentUser.value = user
            Log.d("AuthRepository", "✅ User created successfully: ${user.id}")
            AuthResult.Success(user)

        } catch (e: FirebaseAuthUserCollisionException) {
            Log.e("AuthRepository", "Email collision", e)
            AuthResult.Error("This email is already registered")
        } catch (e: FirebaseAuthWeakPasswordException) {
            Log.e("AuthRepository", "Weak password", e)
            AuthResult.Error("Password is too weak. Please use a stronger password")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e("AuthRepository", "Invalid credentials", e)
            AuthResult.Error("Invalid email format")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign-up failed", e)
            AuthResult.Error(e.message ?: "Failed to create account. Please try again.")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
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
                    DuplicateCheckResult(true, "Email and phone number already registered")
                !emailQuery.isEmpty ->
                    DuplicateCheckResult(true, "This email is already registered")
                !phoneQuery.isEmpty ->
                    DuplicateCheckResult(true, "This phone number is already registered")
                else -> DuplicateCheckResult(false)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking duplicates", e)
            DuplicateCheckResult(false)
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return AuthResult.Error("Sign in failed")

            val user = userRepository.getUser(firebaseUser.uid)
            if (user != null) {
                _currentUser.value = user
                updateLastActive(firebaseUser.uid)
                AuthResult.Success(user)
            } else {
                AuthResult.Error("User profile not found. Please contact support.")
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Error("No account found with this email")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Incorrect password")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign-in failed", e)
            AuthResult.Error(e.message ?: "Login failed. Please try again.")
        }
    }

    suspend fun updateUser(user: User): Boolean {
        return try {
            val updatedUser = user.copy(
                updated_at = Timestamp.now(),
                last_active = Timestamp.now()
            )

            firestore.collection("users")
                .document(user.id)
                .set(updatedUser.toMap())
                .await()

            _currentUser.value = updatedUser
            Log.d("AuthRepository", "✅ User updated successfully")
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update user failed", e)
            false
        }
    }

    // Phone verification methods (keep existing implementation)
    suspend fun sendPhoneVerification(phoneNumber: String, activity: Activity): AuthResult {
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
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
            AuthResult.Success(User(id = "pending"))
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send verification code")
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
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user.toMap())
                    .await()
            }

            _currentUser.value = user
            updateLastActive(firebaseUser.uid)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Invalid verification code")
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        verificationId = null
        resendToken = null
        Log.d("AuthRepository", "User signed out")
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    suspend fun refreshUserData() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            loadUserData(firebaseUser.uid)
            updateLastActive(firebaseUser.uid)
        }
    }
}

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}