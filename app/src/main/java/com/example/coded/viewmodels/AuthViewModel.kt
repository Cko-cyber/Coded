// AuthViewModel.kt
package com.example.coded.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.AuthRepository
import com.example.coded.data.AuthResult
import com.example.coded.data.VerificationState
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Expose repository flows directly (clean & reactive)
    val verificationState: StateFlow<VerificationState> = authRepository.verificationState
    val currentUser = authRepository.currentUser
    val isUserLoggedIn: Boolean
        get() = authRepository.isUserLoggedIn()

    // UI Input Fields
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _firstName = MutableStateFlow("")
    val firstName: StateFlow<String> = _firstName.asStateFlow()

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    // Full name for convenience
    val fullName: StateFlow<String> = combine(_firstName, _lastName) { first, last ->
        "$first $last".trim()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    // Update functions
    fun updateEmail(value: String) { _email.value = value }
    fun updatePassword(value: String) { _password.value = value }
    fun updateConfirmPassword(value: String) { _confirmPassword.value = value }
    fun updatePhoneNumber(value: String) { _phoneNumber.value = value.replace(Regex("[^\\d]"), "") }
    fun updateFirstName(value: String) { _firstName.value = value.trim() }
    fun updateLastName(value: String) { _lastName.value = value.trim() }
    fun updateLocation(value: String) { _location.value = value.trim() }
    fun updateOtpCode(value: String) { _otpCode.value = value }

    fun clearUiState() {
        _uiState.value = AuthUiState.Idle
    }

    fun resetOtpFields() {
        _otpCode.value = ""
        _countdown.value = 0
    }

    fun startCountdown() {
        _countdown.value = 60
        viewModelScope.launch {
            while (_countdown.value > 0) {
                kotlinx.coroutines.delay(1000)
                _countdown.value -= 1
            }
        }
    }

    // Validation
    fun isSignUpFormValid(): Boolean {
        return _firstName.value.isNotBlank() &&
                _lastName.value.isNotBlank() &&
                _phoneNumber.value.length == 8 &&
                _email.value.contains("@") &&
                _location.value.isNotBlank() &&
                _password.value.length >= 6 &&
                _password.value == _confirmPassword.value
    }

    fun isLoginFormValid(): Boolean {
        return _email.value.isNotBlank() && _password.value.length >= 6
    }

    // === Authentication Actions ===

    fun sendOtp(activity: Activity) {
        val fullPhoneNumber = "+268${_phoneNumber.value}"
        authRepository.sendPhoneVerification(fullPhoneNumber, activity)
    }

    fun resendOtp(activity: Activity) {
        val fullPhoneNumber = "+268${_phoneNumber.value}"
        authRepository.resendPhoneVerification(fullPhoneNumber, activity)
    }

    fun verifyOtp() {
        if (_otpCode.value.length != 6) {
            _uiState.value = AuthUiState.Error("Please enter a valid 6-digit code")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.verifyPhoneOTP(_otpCode.value)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Success
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun signUpWithEmail() {
        if (!isSignUpFormValid()) {
            _uiState.value = AuthUiState.Error("Please fill all fields correctly")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signUp(
                email = _email.value,
                password = _password.value,
                mobile_number = "+268${_phoneNumber.value}",
                full_name = fullName.value,
                location = _location.value
            )) {
                AuthResult.Success -> _uiState.value = AuthUiState.Success
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun loginWithEmail() {
        if (!isLoginFormValid()) {
            _uiState.value = AuthUiState.Error("Invalid email or password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signIn(_email.value, _password.value)) {
                AuthResult.Success -> _uiState.value = AuthUiState.Success
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        clearAllFields()
        _uiState.value = AuthUiState.Idle
    }

    fun refreshUserData() {
        authRepository.refreshUserData()
    }

    private fun clearAllFields() {
        _email.value = ""
        _password.value = ""
        _confirmPassword.value = ""
        _phoneNumber.value = ""
        _firstName.value = ""
        _lastName.value = ""
        _location.value = ""
        _otpCode.value = ""
        _countdown.value = 0
    }

    // Helper
    fun getCurrentFirebaseUser(): FirebaseUser? = authRepository.getCurrentFirebaseUser()
    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()
    fun hasCompleteProfile(): Boolean = authRepository.hasCompleteProfile()
}

// UI State
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}