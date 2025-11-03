package com.example.coded.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    navController: NavController,
    listingId: String
) {
    val editListingViewModel: EditListingViewModel = viewModel()
    val listingState = editListingViewModel.listing.collectAsState()
    val isLoading = editListingViewModel.isLoading.collectAsState()
    val error = editListingViewModel.error.collectAsState()

    val listing = listingState.value

    // Form state - initialize with empty values
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var deworming by remember { mutableStateOf("") }
    var vaccinationStatus by remember { mutableStateOf("") }
    var fullDetails by remember { mutableStateOf("") }

    // Initialize form when listing loads
    LaunchedEffect(listing) {
        listing?.let {
            breed = it.breed
            age = it.age
            price = it.price.toString()
            location = it.location
            deworming = it.deworming
            vaccinationStatus = it.vaccination_status
            fullDetails = it.full_details
        }
    }

    LaunchedEffect(listingId) {
        println("📝 EditListingScreen: Loading listing $listingId")
        editListingViewModel.loadListing(listingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (listing != null) "Edit ${listing.breed}"
                        else "Edit Listing"
                    )
                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading.value -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF013B33))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading listing...")
                        }
                    }
                }
                error.value != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = "Error",
                                modifier = Modifier.size(64.dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Error: ${error.value}")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    editListingViewModel.clearError()
                                    editListingViewModel.loadListing(listingId)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF013B33)
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                listing != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Edit Listing Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Breed
                        OutlinedTextField(
                            value = breed,
                            onValueChange = { breed = it },
                            label = { Text("Breed *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Age (String field based on your Listing class)
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("e.g., 12 months, 2 years") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Price
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price (E) *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Location
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Deworming
                        OutlinedTextField(
                            value = deworming,
                            onValueChange = { deworming = it },
                            label = { Text("Deworming Status") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("e.g., Dewormed, Not dewormed") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Vaccination Status
                        OutlinedTextField(
                            value = vaccinationStatus,
                            onValueChange = { vaccinationStatus = it },
                            label = { Text("Vaccination Status") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("e.g., Vaccinated, Not vaccinated") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full Details
                        OutlinedTextField(
                            value = fullDetails,
                            onValueChange = { fullDetails = it },
                            label = { Text("Full Details") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            minLines = 4,
                            placeholder = { Text("Additional details about the livestock...") }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Save Button
                        Button(
                            onClick = {
                                if (listing.id != null) {
                                    val updatedListing = listing.copy(
                                        breed = breed,
                                        age = age,
                                        price = price.toLongOrNull() ?: 0L,
                                        location = location,
                                        deworming = deworming,
                                        vaccination_status = vaccinationStatus,
                                        full_details = fullDetails
                                    )
                                    editListingViewModel.updateListing(updatedListing)
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF013B33)
                            ),
                            enabled = breed.isNotEmpty() && age.isNotEmpty() &&
                                    price.isNotEmpty() && location.isNotEmpty()
                        ) {
                            Text("Save Changes", style = MaterialTheme.typography.titleMedium)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cancel Button
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = "Not Found",
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Listing not found")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF013B33)
                                )
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            }
        }
    }
}