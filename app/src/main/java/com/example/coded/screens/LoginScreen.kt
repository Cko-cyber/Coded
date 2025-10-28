package com.example.coded.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coded.R
import com.example.coded.data.AuthRepository
import com.example.coded.data.AuthResult
import com.example.coded.data.VerificationState
import com.example.coded.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, authRepository: AuthRepository) {
    var selectedLoginMethod by remember { mutableStateOf(LoginMethod.PHONE) }

    // Email login state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Phone login state
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }

    // Common state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe verification state for phone authentication
    val verificationState by authRepository.verificationState.collectAsState()

    // Countdown timer for OTP resend
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }

    // Handle verification state changes
    LaunchedEffect(verificationState) {
        when (verificationState) {
            is VerificationState.CodeSent -> {
                isLoading = false
                isOtpSent = true
                countdown = 60
                val fullPhoneNumber = "+268${phoneNumber.filter { it.isDigit() }}"
                successMessage = "OTP sent to $fullPhoneNumber"
                errorMessage = null
            }
            is VerificationState.Verified -> {
                isLoading = false
                navController.navigate(Screen.MainHome.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is VerificationState.Error -> {
                isLoading = false
                errorMessage = (verificationState as VerificationState.Error).message
                successMessage = null
            }
            is VerificationState.Loading -> {
                isLoading = true
                errorMessage = null
                successMessage = null
            }
            else -> {
                // Idle state - do nothing
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.herdmat_logo),
            contentDescription = "Herdmat Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 32.dp)
        )

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF013B33)
        )

        Text(
            text = "Login to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // Login Method Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                LoginMethodTab(
                    text = "Mobile Login",
                    isSelected = selectedLoginMethod == LoginMethod.PHONE,
                    onClick = { selectedLoginMethod = LoginMethod.PHONE },
                    modifier = Modifier.weight(1f)
                )
                LoginMethodTab(
                    text = "Email Login",
                    isSelected = selectedLoginMethod == LoginMethod.EMAIL,
                    onClick = { selectedLoginMethod = LoginMethod.EMAIL },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (selectedLoginMethod) {
            LoginMethod.EMAIL -> {
                EmailLoginForm(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                    isLoading = isLoading,
                    onLoginClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            try {
                                val result = authRepository.signIn(email, password)
                                isLoading = false
                                when (result) {
                                    is AuthResult.Success -> {
                                        navController.navigate(Screen.MainHome.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "Login failed. Please try again."
                            }
                        }
                    },
                    onForgotPasswordClick = {
                        errorMessage = "Forgot password feature coming soon!"
                    }
                )
            }
            LoginMethod.PHONE -> {
                PhoneLoginForm(
                    phoneNumber = phoneNumber,
                    onPhoneNumberChange = { phoneNumber = it },
                    otpCode = otpCode,
                    onOtpChange = { otpCode = it },
                    isOtpSent = isOtpSent,
                    countdown = countdown,
                    isLoading = isLoading,
                    onSendOtpClick = {
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        val fullPhoneNumber = "+268${phoneNumber.filter { it.isDigit() }}"
                        authRepository.sendPhoneVerification(fullPhoneNumber, context as Activity)
                    },
                    onVerifyOtpClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            try {
                                val result = authRepository.verifyPhoneOTP(otpCode)
                                when (result) {
                                    is AuthResult.Success -> {
                                        // Navigation will be handled by the verification state observer
                                    }
                                    is AuthResult.Error -> {
                                        isLoading = false
                                        errorMessage = result.message
                                    }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "OTP verification failed. Please try again."
                            }
                        }
                    },
                    onResendOtpClick = {
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        val fullPhoneNumber = "+268${phoneNumber.filter { it.isDigit() }}"
                        authRepository.resendPhoneVerification(fullPhoneNumber, context as Activity)
                    }
                )
            }
        }

        // Success Message
        successMessage?.let {
            Text(
                text = it,
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        // Error Message
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Sign Up",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF013B33),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }
    }
}

@Composable
fun LoginMethodTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = if (isSelected) Color(0xFF013B33) else Color.Gray,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 14.sp
    )
}

@Composable
fun EmailLoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF013B33),
                focusedLabelColor = Color(0xFF013B33)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF013B33),
                focusedLabelColor = Color(0xFF013B33)
            )
        )

        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF013B33),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp)
                .clickable(onClick = onForgotPasswordClick)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF013B33)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Login with Email",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PhoneLoginForm(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isOtpSent: Boolean,
    countdown: Int,
    isLoading: Boolean,
    onSendOtpClick: () -> Unit,
    onVerifyOtpClick: () -> Unit,
    onResendOtpClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Phone Number Field with Eswatini prefix
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                val cleaned = newValue.filter { it.isDigit() }
                if (cleaned.length <= 8) {
                    onPhoneNumberChange(cleaned.formatPhoneNumber())
                }
            },
            label = { Text("Mobile Number") },
            leadingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+268",
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray)
                }
            },
            placeholder = { Text("7612 3456") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF013B33),
                focusedLabelColor = Color(0xFF013B33)
            )
        )

        // Phone number format hint
        Text(
            text = "Enter your 8-digit Eswatini number (76, 78, or 79)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isOtpSent) {
            // OTP Field
            OutlinedTextField(
                value = otpCode,
                onValueChange = { newValue ->
                    val cleaned = newValue.filter { it.isDigit() }
                    if (cleaned.length <= 6) {
                        onOtpChange(cleaned)
                    }
                },
                label = { Text("OTP Code") },
                leadingIcon = { Icon(Icons.Default.Sms, contentDescription = null) },
                placeholder = { Text("Enter 6-digit code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF013B33),
                    focusedLabelColor = Color(0xFF013B33)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Resend OTP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (countdown > 0) "Resend OTP in $countdown seconds" else "Didn't receive code?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                TextButton(
                    onClick = onResendOtpClick,
                    enabled = countdown == 0 && !isLoading
                ) {
                    Text("Resend OTP")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verify OTP Button
            Button(
                onClick = onVerifyOtpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && otpCode.length == 6
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Verify OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Send OTP Button
            Button(
                onClick = onSendOtpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && isValidEswatiniPhoneNumber(phoneNumber)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Send OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Extension function to format Eswatini phone numbers
private fun String.formatPhoneNumber(): String {
    val cleaned = this.filter { it.isDigit() }
    return when {
        cleaned.length <= 4 -> cleaned
        cleaned.length <= 8 -> "${cleaned.substring(0, 4)} ${cleaned.substring(4)}"
        else -> "${cleaned.substring(0, 4)} ${cleaned.substring(4, 8)}"
    }.trim()
}

// Function to validate Eswatini phone numbers
private fun isValidEswatiniPhoneNumber(phoneNumber: String): Boolean {
    val cleaned = phoneNumber.filter { it.isDigit() }
    if (cleaned.length != 8) return false
    val validPrefixes = listOf("76", "78", "79")
    return validPrefixes.any { cleaned.startsWith(it) }
}

// Login method enum
enum class LoginMethod {
    EMAIL, PHONE
}