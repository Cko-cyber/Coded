package com.example.coded.data

import android.content.Context
import com.example.coded.CodedApplication
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Simple interface for splash screen
interface AuthRepository {
    val currentUser: StateFlow<UserInfo?>
}

class OasisAuthRepository(private val context: Context) : AuthRepository {
    private val supabase = CodedApplication.supabase

    // For splash screen compatibility
    private val _currentUser = MutableStateFlow(supabase.auth.currentUserOrNull())
    override val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    init {
        // Listen for auth changes
        // Note: Supabase doesn't have a direct listener like Firebase
        // You'll need to update this manually after sign in/out
    }

    // Add other auth methods as needed
    suspend fun signOut() {
        supabase.auth.signOut()
        _currentUser.value = null
    }

    suspend fun refreshUser() {
        _currentUser.value = supabase.auth.currentUserOrNull()
    }
}