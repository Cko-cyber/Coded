// File: app/src/main/java/com/example/coded/viewmodels/ProviderAuthViewModel.kt
package com.example.coded.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coded.data.HerdmatAuthRepository
import com.example.coded.data.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProviderAuthUiState(
    // Form fields
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val phone: String = "",
    val location: String = "",
    val skillsInput: String = "",
    val skills: List<String> = emptyList(),
    val hourlyRate: String = "",

    // UI state
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: String? = null
)

class ProviderAuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderAuthUiState())
    val uiState: StateFlow<ProviderAuthUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword)
    }

    fun updateFullName(fullName: String) {
        _uiState.value = _uiState.value.copy(fullName = fullName)
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone)
    }

    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }

    fun updateSkillsInput(skillsInput: String) {
        _uiState.value = _uiState.value.copy(skillsInput = skillsInput)

        // Parse skills when input changes
        if (skillsInput.contains(",") || skillsInput.endsWith(" ")) {
            val parsedSkills = skillsInput.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            if (parsedSkills.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(skills = parsedSkills)
            }
        }
    }

    fun updateHourlyRate(hourlyRate: String) {
        _uiState.value = _uiState.value.copy(hourlyRate = hourlyRate)
    }

    fun removeSkill(skill: String) {
        val currentSkills = _uiState.value.skills.toMutableList()
        currentSkills.remove(skill)
        _uiState.value = _uiState.value.copy(skills = currentSkills)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(passwordVisible = !_uiState.value.passwordVisible)
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            confirmPasswordVisible = !_uiState.value.confirmPasswordVisible
        )
    }

    fun clearForm() {
        _uiState.value = ProviderAuthUiState()
    }

    fun validateForm(isLoginMode: Boolean): Boolean {
        return if (isLoginMode) {
            _uiState.value.email.isNotBlank() && _uiState.value.password.isNotBlank()
        } else {
            _uiState.value.email.isNotBlank() &&
                    _uiState.value.password.isNotBlank() &&
                    _uiState.value.confirmPassword.isNotBlank() &&
                    _uiState.value.fullName.isNotBlank() &&
                    _uiState.value.phone.isNotBlank() &&
                    _uiState.value.location.isNotBlank() &&
                    _uiState.value.skills.isNotEmpty() &&
                    _uiState.value.password == _uiState.value.confirmPassword &&
                    _uiState.value.password.length >= 6
        }
    }

    fun loginProvider(
        authRepository: HerdmatAuthRepository,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.providerLogin(
                _uiState.value.email,
                _uiState.value.password
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSuccess = "Login successful!")
                    onComplete(true)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                    onComplete(false)
                }
            }
        }
    }

    fun signUpProvider(
        authRepository: HerdmatAuthRepository,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val hourlyRate = _uiState.value.hourlyRate.toDoubleOrNull()

            val result = authRepository.providerSignUp(
                email = _uiState.value.email,
                password = _uiState.value.password,
                fullName = _uiState.value.fullName,
                phone = _uiState.value.phone,
                location = _uiState.value.location,
                skills = _uiState.value.skills,
                hourlyRate = hourlyRate
            )

            _uiState.value = _uiState.value.copy(isLoading = false)

            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSuccess = "Account created! Awaiting verification."
                    )
                    onComplete(true)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                    onComplete(false)
                }
            }
        }
    }
}