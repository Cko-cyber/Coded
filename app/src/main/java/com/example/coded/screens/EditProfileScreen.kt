package com.example.coded.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.coded.R
import com.example.coded.data.AuthRepository
import com.example.coded.data.ProfilePictureUploader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, authRepository: AuthRepository) {
    val context = LocalContext.current
    val currentUser by authRepository.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(currentUser?.full_name ?: "") }
    var mobileNumber by remember { mutableStateOf(currentUser?.mobile_number ?: "") }
    var location by remember { mutableStateOf(currentUser?.location ?: "") }
    var profilePicUri by remember { mutableStateOf<Uri?>(null) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingPic by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profilePicUri = uri
    }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            fullName = it.full_name
            mobileNumber = it.mobile_number
            location = it.location
        }
    }

    // Remove Profile Picture Dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Remove Profile Picture") },
            text = { Text("Are you sure you want to remove your profile picture?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        coroutineScope.launch {
                            isLoading = true
                            val updatedUser = currentUser!!.copy(profile_pic = "")
                            val success = authRepository.updateUser(updatedUser)
                            if (success) {
                                successMessage = "Profile picture removed"
                                profilePicUri = null
                            } else {
                                errorMessage = "Failed to remove profile picture"
                            }
                            isLoading = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentUser == null) {
                CircularProgressIndicator()
                return@Scaffold
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture Section
            Box(
                modifier = Modifier.size(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF013B33))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(profilePicUri),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!currentUser?.profile_pic.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(currentUser?.profile_pic),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                    }

                    if (isUploadingPic) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = uploadProgress,
                                color = Color.White
                            )
                        }
                    }
                }

                // Edit icon button
                FloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(40.dp),
                    containerColor = Color(0xFF013B33)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change Picture",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Remove button (only show if there's a profile picture)
                if (!currentUser?.profile_pic.isNullOrEmpty() || profilePicUri != null) {
                    FloatingActionButton(
                        onClick = { showRemoveDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(40.dp),
                        containerColor = Color.Red
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove Picture",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap to change or remove picture",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
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
                leadingIcon = { Icon(Icons.Default.Phone, null) },
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
                value = currentUser?.email ?: "",
                onValueChange = {},
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true
            )

            Text(
                text = "Email cannot be changed",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF013B33),
                    focusedLabelColor = Color(0xFF013B33)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error/Success Messages
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            successMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = Color(0xFF2E7D32))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Save Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        when {
                            fullName.isBlank() -> errorMessage = "Full name is required"
                            fullName.length < 2 -> errorMessage = "Full name must be at least 2 characters"
                            mobileNumber.isBlank() -> errorMessage = "Mobile number is required"
                            location.isBlank() -> errorMessage = "Location is required"
                            else -> {
                                isLoading = true
                                errorMessage = null
                                successMessage = null

                                var profilePicUrl = currentUser?.profile_pic ?: ""

                                // Upload profile picture if changed
                                if (profilePicUri != null) {
                                    isUploadingPic = true
                                    val uploadResult = ProfilePictureUploader.uploadProfilePicture(
                                        context = context,
                                        uri = profilePicUri!!,
                                        userId = currentUser!!.id
                                    ) { progress ->
                                        uploadProgress = progress
                                    }
                                    isUploadingPic = false

                                    if (uploadResult.isSuccess) {
                                        profilePicUrl = uploadResult.getOrNull() ?: profilePicUrl
                                    } else {
                                        errorMessage = "Failed to upload profile picture"
                                        isLoading = false
                                        return@launch
                                    }
                                }

                                val updatedUser = currentUser!!.copy(
                                    full_name = fullName.trim(),
                                    mobile_number = mobileNumber.trim(),
                                    location = location.trim(),
                                    profile_pic = profilePicUrl
                                )

                                val success = authRepository.updateUser(updatedUser)

                                if (success) {
                                    successMessage = "Profile updated successfully!"
                                    kotlinx.coroutines.delay(1500)
                                    navController.popBackStack()
                                } else {
                                    errorMessage = "Failed to update profile"
                                }

                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF013B33)),
                enabled = !isLoading && !isUploadingPic
            ) {
                if (isLoading || isUploadingPic) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Default.Save, "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF013B33)
                )
            ) {
                Text("Cancel")
            }
        }
    }
}