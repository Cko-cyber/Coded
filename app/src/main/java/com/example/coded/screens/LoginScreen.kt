package com.example.coded.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coded.R
import com.example.coded.data.AuthRepository
import com.example.coded.navigation.Screen
import com.example.coded.viewmodels.AuthUiState
import com.example.coded.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

enum class LoginMethod { EMAIL, PHONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val verificationState by viewModel.verificationState.collectAsState()

    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val isOtpSent by viewModel.isOtpSent.collectAsState()
    val countdown by viewModel.countdown.collectAsState()

    var selectedMethod by remember { mutableStateOf(LoginMethod.PHONE) }
    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                navController.navigate(Screen.MainHome.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
                viewModel.clearState()
            }
            else -> {}
        }
    }

    LaunchedEffect(verificationState) {
        when (verificationState) {
            is com.example.coded.data.VerificationState.CodeSent -> {
                viewModel.startCountdown()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isOtpSent) {
                TopAppBar(
                    title = { Text("Verify OTP") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.resetOtp()
                        }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF013B33),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF013B33))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.herdmat_logo),
                contentDescription = "Herdmat Logo",
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = if (isOtpSent) "Enter OTP" else "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = if (isOtpSent) "Check your phone for the code" else "Login to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (!isOtpSent) {
                // Login Method Tabs
                TabRow(
                    selectedTabIndex = if (selectedMethod == LoginMethod.PHONE) 0 else 1,
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = selectedMethod == LoginMethod.PHONE,
                        onClick = { selectedMethod = LoginMethod.PHONE },
                        text = { Text("Phone") }
                    )
                    Tab(
                        selected = selectedMethod == LoginMethod.EMAIL,
                        onClick = { selectedMethod = LoginMethod.EMAIL },
                        text = { Text("Email") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            when {
                isOtpSent -> {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) viewModel.updateOtpCode(it.filter { char -> char.isDigit() }) },
                        label = { Text("6-Digit OTP", color = Color.White) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFD700),
                            unfocusedLabelColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (countdown > 0) "Resend in $countdown s" else "Didn't receive?",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = {
                                activity?.let { viewModel.resendOtp(it) }
                            },
                            enabled = countdown == 0 && activity != null
                        ) {
                            Text("Resend", color = Color(0xFFFFD700))
                        }
                    }

                    Button(
                        onClick = { viewModel.verifyOtp() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        enabled = otpCode.length == 6 && uiState !is AuthUiState.Loading
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(color = Color(0xFF013B33), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Verify OTP", color = Color(0xFF013B33), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                selectedMethod == LoginMethod.PHONE && !isOtpSent -> {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { viewModel.updatePhoneNumber(it.filter { char -> char.isDigit() }.take(8)) },
                        label = { Text("Phone Number", color = Color.White) },
                        leadingIcon = { Text("+268", color = Color.White) },
                        placeholder = { Text("76123456", color = Color.White.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFD700),
                            unfocusedLabelColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            activity?.let { viewModel.sendOtp(it) }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        enabled = isValidEswatiniPhoneNumber(phoneNumber) &&
                                uiState !is AuthUiState.Loading &&
                                activity != null
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(color = Color(0xFF013B33), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Send OTP", color = Color(0xFF013B33), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                selectedMethod == LoginMethod.EMAIL && !isOtpSent -> {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = { Text("Email", color = Color.White) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFD700),
                            unfocusedLabelColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password", color = Color.White) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = Color.White
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFFFD700),
                            unfocusedLabelColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.loginWithEmail() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        enabled = email.isNotBlank() && password.length >= 6 && uiState !is AuthUiState.Loading
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(color = Color(0xFF013B33), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", color = Color(0xFF013B33), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isOtpSent) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text("Don't have an account? ", color = Color.White.copy(alpha = 0.8f))
                    Text(
                        "Sign Up",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.Signup.route)
                        }
                    )
                }
            }
        }
    }
}

private fun isValidEswatiniPhoneNumber(phone: String): Boolean {
    val cleaned = phone.filter { it.isDigit() }
    return cleaned.length == 8 && listOf("76", "78", "79").any { cleaned.startsWith(it) }
}