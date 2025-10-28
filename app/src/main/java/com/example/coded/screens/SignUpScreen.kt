package com.example.coded.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController, authRepository: AuthRepository) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF013B33)) // ✅ Dark green background
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.herdmat_logo),
            contentDescription = "Herdmat Logo",
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 24.dp)
        )

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White // ✅ White text on dark background
        )

        Text(
            text = "Join the livestock trading community",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f), // ✅ Semi-transparent white
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // First Name
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name", color = Color.White) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                cursorColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last Name
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name", color = Color.White) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                cursorColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number", color = Color.White) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                cursorColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.White) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                cursorColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Location
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location", color = Color.White) },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                cursorColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = Color.White) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.White
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                cursorColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password", color = Color.White) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                        tint = Color.White
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color(0xFFFFD700),
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                cursorColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error Message
        errorMessage?.let {
            Surface(
                color = Color.Red.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = it,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Sign Up Button
        Button(
            onClick = {
                when {
                    password != confirmPassword -> {
                        errorMessage = "Passwords do not match"
                    }
                    password.length < 6 -> {
                        errorMessage = "Password must be at least 6 characters"
                    }
                    else -> {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                // ✅ CORRECTED: Use the proper parameter names
                                val result = authRepository.signUp(
                                    email = email,
                                    password = password,
                                    mobile_number = phone,
                                    full_name = "$firstName $lastName",
                                    location = location
                                )
                                isLoading = false
                                when (result) {
                                    is AuthResult.Success -> {
                                        navController.navigate("main_home") {
                                            popUpTo("signup") { inclusive = true }
                                        }
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "Error occurred during signup."
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading &&
                    firstName.isNotEmpty() &&
                    lastName.isNotEmpty() &&
                    phone.isNotEmpty() &&
                    email.isNotEmpty() &&
                    password.isNotEmpty() &&
                    confirmPassword.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF013B33),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Sign Up",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF013B33)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "Login",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    navController.popBackStack()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}