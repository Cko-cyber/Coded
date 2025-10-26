package com.example.coded.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coded.data.AuthRepository
import com.example.coded.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, authRepository: AuthRepository) {
    val currentUser by authRepository.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var mobileNumber by remember { mutableStateOf(currentUser?.mobileNumber ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Initialize fields when user data loads
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            fullName = user.fullName
            mobileNumber = user.mobileNumber
            email = user.email
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Update Your Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF013B33),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF013B33),
                    focusedLabelColor = Color(0xFF013B33)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mobile Number
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Mobile Number") },
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF013B33),
                    focusedLabelColor = Color(0xFF013B33)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email (read-only)
            OutlinedTextField(
                value = email,
                onValueChange = { },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color.Gray,
                    disabledLabelColor = Color.Gray
                )
            )

            Text(
                text = "Email cannot be changed",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Success Message
            successMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error Message
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Save Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        successMessage = null

                        try {
                            if (currentUser == null) {
                                errorMessage = "User not found"
                                return@launch
                            }

                            val updatedUser = currentUser!!.copy(
                                fullName = fullName,
                                mobileNumber = mobileNumber,
                                updated_at = com.google.firebase.Timestamp.now()
                            )

                            val success = authRepository.updateUser(updatedUser)

                            if (success) {
                                successMessage = "Profile updated successfully!"
                                // Refresh user data
                                authRepository.refreshUserData()
                            } else {
                                errorMessage = "Failed to update profile"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                ),
                enabled = !isLoading && fullName.isNotBlank() && mobileNumber.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Save, "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel Button
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Account Actions Section
            Text(
                text = "Account Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF013B33),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Change Password (placeholder)
            OutlinedButton(
                onClick = {
                    // TODO: Implement password change
                    errorMessage = "Password change feature coming soon!"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, "Password")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Password")
            }
        }
    }
}