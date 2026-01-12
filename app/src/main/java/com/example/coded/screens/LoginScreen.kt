package com.example.coded.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coded.R
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.coded.data.OasisAuthRepository
import com.example.coded.ui.theme.*
import kotlinx.coroutines.delay

// ==================== ENUMS AND DATA CLASSES ====================

enum class LoginMethod { EMAIL, PHONE }

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

// ==================== VIEW MODEL ====================

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
        viewModelScope.launch {
            delay(1000)
            _uiState.value = AuthUiState.Success(_userType.value ?: UserType.CLIENT)
        }
    }

    fun loginWithEmail() {
        _uiState.value = AuthUiState.Loading
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

// ==================== LOGIN SCREEN COMPOSABLE ====================

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

    // Navigation handling
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                val successState = uiState as AuthUiState.Success
                when (successState.userType) {
                    UserType.PROVIDER -> {
                        navController.navigate("provider/home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                    UserType.ADMIN -> {
                        navController.navigate("admin/dashboard") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                    else -> {
                        navController.navigate("guest/home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
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
                    title = { Text("Verify OTP", color = OasisWhite) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.resetOtp() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = OasisWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OasisGreen
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            OasisGreen,
                            OasisDark
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // FULL LOGO (Icon + Name)
                Image(
                    painter = painterResource(id = R.drawable.oasis_logo), // Using existing logo
                    contentDescription = "Oasis Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isOtpSent) "Enter OTP" else "Welcome to Oasis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OasisWhite
                )

                Text(
                    text = if (isOtpSent) "Check your phone for the code"
                    else "Login to manage your account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OasisMint,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // User Type Selection (only if not OTP sent)
                if (!isOtpSent) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable { showUserTypeDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = OasisWhite.copy(alpha = 0.15f)
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
                                    color = OasisGray
                                )
                                Text(
                                    text = userType.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = OasisMint
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Change user type",
                                tint = OasisMint
                            )
                        }
                    }
                }

                // Login Method Tabs (only for ADMIN/PROVIDER, not for OTP)
                if (!isOtpSent && userType != UserType.CLIENT) {
                    TabRow(
                        selectedTabIndex = if (selectedMethod == LoginMethod.PHONE) 0 else 1,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = OasisMint,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Tab(
                            selected = selectedMethod == LoginMethod.PHONE,
                            onClick = { selectedMethod = LoginMethod.PHONE },
                            text = { Text("Phone", color = if (selectedMethod == LoginMethod.PHONE) OasisMint else OasisGray) }
                        )
                        Tab(
                            selected = selectedMethod == LoginMethod.EMAIL,
                            onClick = { selectedMethod = LoginMethod.EMAIL },
                            text = { Text("Email", color = if (selectedMethod == LoginMethod.EMAIL) OasisMint else OasisGray) }
                        )
                    }
                }

                // Form Fields
                when {
                    isOtpSent -> {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { newValue ->
                                if (newValue.length <= 6) {
                                    viewModel.updateOtpCode(newValue.filter { it.isDigit() })
                                }
                            },
                            label = { Text("6-Digit OTP", color = OasisGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OasisTeal,
                                unfocusedBorderColor = OasisGray.copy(alpha = 0.5f),
                                focusedTextColor = OasisWhite,
                                unfocusedTextColor = OasisWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (countdown > 0) "Resend in $countdown s" else "Didn't receive?",
                                color = OasisGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = { activity?.let { viewModel.resendOtp(it) } },
                                enabled = countdown == 0 && activity != null
                            ) {
                                Text("Resend", color = OasisTeal)
                            }
                        }

                        Button(
                            onClick = { viewModel.verifyOtp() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OasisTeal),
                            enabled = otpCode.length == 6 && uiState !is AuthUiState.Loading
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(color = OasisWhite, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Verify OTP", color = OasisWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    selectedMethod == LoginMethod.PHONE && !isOtpSent && userType != UserType.CLIENT -> {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it.isDigit() }
                                viewModel.updatePhoneNumber(filtered.take(8))
                            },
                            label = { Text("Phone Number", color = OasisGray) },
                            leadingIcon = { Text("+268", color = OasisMint) },
                            placeholder = { Text("76123456", color = OasisGray.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OasisTeal,
                                unfocusedBorderColor = OasisGray.copy(alpha = 0.5f),
                                focusedTextColor = OasisWhite,
                                unfocusedTextColor = OasisWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { activity?.let { viewModel.sendOtp(it) } },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OasisTeal),
                            enabled = isValidEswatiniPhoneNumber(phoneNumber) &&
                                    uiState !is AuthUiState.Loading &&
                                    activity != null
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(color = OasisWhite, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Send OTP", color = OasisWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    selectedMethod == LoginMethod.EMAIL && !isOtpSent && userType != UserType.CLIENT -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = { Text("Email", color = OasisGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OasisTeal,
                                unfocusedBorderColor = OasisGray.copy(alpha = 0.5f),
                                focusedTextColor = OasisWhite,
                                unfocusedTextColor = OasisWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { viewModel.updatePassword(it) },
                            label = { Text("Password", color = OasisGray) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(
                                        text = if (passwordVisible) "Hide" else "Show",
                                        color = OasisMint,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OasisTeal,
                                unfocusedBorderColor = OasisGray.copy(alpha = 0.5f),
                                focusedTextColor = OasisWhite,
                                unfocusedTextColor = OasisWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.loginWithEmail() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OasisTeal),
                            enabled = email.isNotBlank() && password.length >= 6 && uiState !is AuthUiState.Loading
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(color = OasisWhite, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Login", color = OasisWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!isOtpSent) {
                    // Sign Up option (for ADMIN/PROVIDER)
                    if (userType != UserType.CLIENT) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            Text("Don't have an account? ", color = OasisGray)
                            Text(
                                "Sign Up",
                                color = OasisTeal,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (userType == UserType.PROVIDER) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Provider onboarding available on our website")
                                        }
                                    } else {
                                        navController.navigate("guest/home")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Guest/Client Button
                    Button(
                        onClick = {
                            viewModel.updateUserType(UserType.CLIENT)
                            navController.navigate("guest/home") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = OasisMint
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OasisMint)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue as Guest (Client)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // User Type Dialog
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
                                containerColor = if (userType == type) OasisGreen.copy(alpha = 0.3f) else OasisWhite
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
                                    tint = OasisTeal
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = type.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = OasisGreen
                                    )
                                    Text(
                                        text = type.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OasisGrayDark
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserTypeDialog = false }) {
                    Text("Cancel", color = OasisTeal)
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