package com.example.coded.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.coded.R
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coded.data.OasisAuthRepository
import kotlinx.coroutines.delay

enum class LoginMethod { EMAIL, PHONE }

// Define AuthUiState here since it doesn't exist elsewhere
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userType: UserType) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class VerificationState {
    object Idle : VerificationState()
    object CodeSent : VerificationState()
    object Verified : VerificationState()
    data class Error(val message: String) : VerificationState()
}

enum class UserType {
    CLIENT, PROVIDER, ADMIN
}

val UserType.displayName: String
    get() = when (this) {
        UserType.CLIENT -> "Client"
        UserType.PROVIDER -> "Service Provider"
        UserType.ADMIN -> "Admin"
    }

val UserType.description: String
    get() = when (this) {
        UserType.CLIENT -> "Request services without creating an account"
        UserType.PROVIDER -> "Offer services and earn money"
        UserType.ADMIN -> "Manage platform and verify jobs"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authRepository: OasisAuthRepository,
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    // Use observeAsState with initial values
    val uiState by viewModel.uiState.observeAsState(initial = AuthUiState.Idle)
    val verificationState by viewModel.verificationState.observeAsState(initial = VerificationState.Idle)

    val email by viewModel.email.observeAsState(initial = "")
    val password by viewModel.password.observeAsState(initial = "")
    val phoneNumber by viewModel.phoneNumber.observeAsState(initial = "")
    val otpCode by viewModel.otpCode.observeAsState(initial = "")
    val isOtpSent by viewModel.isOtpSent.observeAsState(initial = false)
    val countdown by viewModel.countdown.observeAsState(initial = 0)
    val userType by viewModel.userType.observeAsState(initial = UserType.CLIENT)

    var selectedMethod by remember { mutableStateOf(LoginMethod.PHONE) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showUserTypeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // FIXED: Simplified navigation to avoid popUpTo issues
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                val successState = uiState as AuthUiState.Success
                // Simple navigation without popUpTo to avoid errors
                when (successState.userType) {
                    UserType.PROVIDER -> {
                        navController.navigate("provider/home") {
                            // Clear back stack to prevent going back to login
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }
                    UserType.ADMIN -> {
                        navController.navigate("admin/dashboard") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }
                    else -> {
                        navController.navigate("guest/home") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }
                }
            }
            is AuthUiState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
                }
                viewModel.clearState()
            }
            else -> {}
        }
    }

    LaunchedEffect(verificationState) {
        when (verificationState) {
            is VerificationState.CodeSent -> {
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
                        containerColor = Color(0xFF4CAF50),
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
                .background(Color(0xFF4CAF50))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Replace with your actual logo resource
            Image(
                painter = painterResource(id = R.drawable.oasis_logo), // Replace with your logo
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = if (isOtpSent) "Enter OTP" else "Welcome to Oasis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = if (isOtpSent) "Check your phone for the code"
                else "Login to manage your account",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // User Type Selection - Only for ADMIN/PROVIDER
            if (!isOtpSent) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { showUserTypeDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Login as",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = userType.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Change user type")
                    }
                }
            }

            if (!isOtpSent) {
                // Login Method Tabs - Only for ADMIN/PROVIDER
                if (userType != UserType.CLIENT) {
                    TabRow(
                        selectedTabIndex = if (selectedMethod == LoginMethod.PHONE) 0 else 1,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
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
                }
            }

            when {
                isOtpSent -> {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { newValue ->
                            if (newValue.length <= 6) {
                                viewModel.updateOtpCode(newValue.filter { char -> char.isDigit() })
                            }
                        },
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
                            CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Verify OTP", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Phone login - only for ADMIN/PROVIDER
                selectedMethod == LoginMethod.PHONE && !isOtpSent && userType != UserType.CLIENT -> {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { char -> char.isDigit() }
                            viewModel.updatePhoneNumber(filtered.take(8))
                        },
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
                            CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Send OTP", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Email login - only for ADMIN/PROVIDER
                selectedMethod == LoginMethod.EMAIL && !isOtpSent && userType != UserType.CLIENT -> {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { newValue -> viewModel.updateEmail(newValue) },
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
                        onValueChange = { newValue -> viewModel.updatePassword(newValue) },
                        label = { Text("Password", color = Color.White) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                // Simple text toggle
                                Text(
                                    text = if (passwordVisible) "Hide" else "Show",
                                    color = Color.White,
                                    modifier = Modifier.padding(end = 8.dp)
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
                            CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isOtpSent) {
                // For ADMIN/PROVIDER - show sign up option
                if (userType != UserType.CLIENT) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text("Don't have an account? ", color = Color.White.copy(alpha = 0.8f))
                        Text(
                            "Sign Up",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                if (userType == UserType.PROVIDER) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Provider onboarding is available on our website")
                                    }
                                } else {
                                    // Simple navigation without parameters
                                    navController.navigate("guest/home")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Guest/Client login button - FIXED: line ~372
                Button(
                    onClick = {
                        viewModel.updateUserType(UserType.CLIENT)
                        // FIXED: Navigate to guest home first, not directly to create job
                        navController.navigate("guest/home") {
                            // Clear the back stack
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue as Guest (Client)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // User Type Selection Dialog - Only ADMIN and PROVIDER
    if (showUserTypeDialog) {
        AlertDialog(
            onDismissRequest = { showUserTypeDialog = false },
            title = { Text("Select User Type") },
            text = {
                Column {
                    listOf(UserType.ADMIN, UserType.PROVIDER).forEach { type ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.updateUserType(type)
                                    showUserTypeDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (userType == type) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        UserType.PROVIDER -> Icons.Default.Build
                                        UserType.ADMIN -> Icons.Default.Lock
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = type.displayName,
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = type.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = type.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserTypeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper function
private fun isValidEswatiniPhoneNumber(phone: String): Boolean {
    val cleaned = phone.filter { it.isDigit() }
    return cleaned.length == 8 && listOf("76", "78", "79").any { cleaned.startsWith(it) }
}

// Add AuthViewModel class at the bottom
class AuthViewModel : ViewModel() {
    private val _userType = MutableLiveData(UserType.CLIENT)
    val userType: LiveData<UserType> get() = _userType

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> get() = _uiState

    private val _verificationState = MutableLiveData<VerificationState>(VerificationState.Idle)
    val verificationState: LiveData<VerificationState> get() = _verificationState

    private val _email = MutableLiveData("")
    val email: LiveData<String> get() = _email

    private val _password = MutableLiveData("")
    val password: LiveData<String> get() = _password

    private val _phoneNumber = MutableLiveData("")
    val phoneNumber: LiveData<String> get() = _phoneNumber

    private val _otpCode = MutableLiveData("")
    val otpCode: LiveData<String> get() = _otpCode

    private val _isOtpSent = MutableLiveData(false)
    val isOtpSent: LiveData<Boolean> get() = _isOtpSent

    private val _countdown = MutableLiveData(0)
    val countdown: LiveData<Int> get() = _countdown

    fun updateUserType(type: UserType) {
        _userType.value = type
    }

    fun updateEmail(email: String) {
        _email.value = email
    }

    fun updatePassword(password: String) {
        _password.value = password
    }

    fun updatePhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }

    fun updateOtpCode(otp: String) {
        _otpCode.value = otp
    }

    fun sendOtp(activity: ComponentActivity) {
        _uiState.value = AuthUiState.Loading
        // Simulate network call
        viewModelScope.launch {
            delay(1000)
            _isOtpSent.value = true
            _verificationState.value = VerificationState.CodeSent
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resendOtp(activity: ComponentActivity) {
        sendOtp(activity)
    }

    fun verifyOtp() {
        _uiState.value = AuthUiState.Loading
        // Simulate verification
        viewModelScope.launch {
            delay(1000)
            _uiState.value = AuthUiState.Success(_userType.value ?: UserType.CLIENT)
        }
    }

    fun loginWithEmail() {
        _uiState.value = AuthUiState.Loading
        // Simulate login
        viewModelScope.launch {
            delay(1000)
            _uiState.value = AuthUiState.Success(_userType.value ?: UserType.CLIENT)
        }
    }

    fun resetOtp() {
        _isOtpSent.value = false
        _otpCode.value = ""
        _countdown.value = 0
    }

    fun startCountdown() {
        _countdown.value = 60
        viewModelScope.launch {
            repeat(60) {
                delay(1000)
                _countdown.value = _countdown.value?.minus(1) ?: 0
            }
        }
    }

    fun clearState() {
        _uiState.value = AuthUiState.Idle
    }
}