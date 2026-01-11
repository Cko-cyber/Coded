package com.example.coded.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Simple interface for splash screen
interface AuthRepository {
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?>
}

class OasisAuthRepository(private val context: Context) : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // For splash screen compatibility
    private val _currentUser = MutableStateFlow(auth.currentUser)
    override val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    init {
        // Listen for auth changes
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    // Add other auth methods as needed
    fun signOut() {
        auth.signOut()
    }
}