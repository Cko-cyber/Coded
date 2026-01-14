package com.example.coded.screens.client

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmationScreen(
    navController: NavController,
    jobId: String,
    totalAmount: Double
) {
    val context = LocalContext.current
    val jobRepository = remember { JobRepository(context) }
    val scope = rememberCoroutineScope()   // ✅ Add context
    // State variables
    var isLoading by remember { mutableStateOf(false) }
    var paymentStatus by remember { mutableStateOf<String?>(null) }
    var paymentReference by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var mobileMoneyProvider by remember { mutableStateOf<String?>(null) }
    var mobileNumber by remember { mutableStateOf("") }

    // Eswatini mobile money providers
    val mobileMoneyProviders = listOf(
        "mtn" to "📱 MTN Mobile Money",
        "eswatini_mobile" to "📱 Eswatini Mobile Money",
        "airtel" to "📱 Airtel Money"
    )

    // Generate payment reference
    val paymentRef = remember {
        "PAY${System.currentTimeMillis() % 1000000}${(100..999).random()}"
    }

    // Format date
    val formattedDate = remember {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date())
    }

    // Format amount
    val formattedAmount = remember {
        String.format("E%.2f", totalAmount)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Payment - Eswatini", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Payment Header with Eswatini flag
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🇸🇿", style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Mobile Money Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Job ID: $jobId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Payment Amount Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total Amount Payable",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formattedAmount,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "SZL ${"%.2f".format(totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Payment Reference: $paymentRef",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mobile Money Provider Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Select Your Mobile Money Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mobileMoneyProviders.forEach { (id, label) ->
                            FilterChip(
                                selected = mobileMoneyProvider == id,
                                onClick = { mobileMoneyProvider = id },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mobile Number Input
                    androidx.compose.material3.OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = { Text("Your Mobile Money Number") },
                        placeholder = { Text("e.g., 7612 3456") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = "Phone")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You'll receive a payment request on this number",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Payment Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📱 Payment Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 1
                    InstructionStep(
                        stepNumber = 1,
                        title = "Select your mobile money provider",
                        description = "Choose MTN, Eswatini Mobile, or Airtel"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 2
                    InstructionStep(
                        stepNumber = 2,
                        title = "Enter your mobile number",
                        description = "The number registered with your mobile money"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 3
                    InstructionStep(
                        stepNumber = 3,
                        title = "Click 'Request Payment'",
                        description = "We'll send a payment prompt to your phone"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 4
                    InstructionStep(
                        stepNumber = 4,
                        title = "Enter your PIN",
                        description = "Authorize the payment with your mobile money PIN"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Important Notes for Eswatini
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFFF57C00)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Important Notes for Eswatini Users",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57C00)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "🇸🇿 Payment Reference: $paymentRef",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Ensure you have sufficient balance in your mobile money account",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Standard mobile money fees apply (2% transaction fee)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• You'll receive SMS confirmation from your provider",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Job will be activated immediately after successful payment",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Admin will assign a qualified provider within 24 hours",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Request Payment Button
                Button(
                    onClick = {
                        if (mobileMoneyProvider == null) {
                            showToast(context, "Please select a mobile money provider")
                            return@Button
                        }
                        if (mobileNumber.isEmpty()) {
                            showToast(context, "Please enter your mobile number")
                            return@Button
                        }
                        if (mobileNumber.length < 8) {
                            showToast(context, "Please enter a valid mobile number")
                            return@Button
                        }

                        isLoading = true

                        // Simulate mobile money payment request
                        scope.launch {
                            kotlinx.coroutines.delay(2000) // Simulate API call

                            // In real app, integrate with mobile money API
                            // For demo, simulate successful payment 80% of the time
                            val success = Random().nextFloat() < 0.8f

                            if (success) {
                                // Update job payment status in Firestore
                                val result = jobRepository.updateJobPayment(
                                    jobId = jobId,
                                    paymentStatus = "paid",
                                    transactionId = paymentRef,
                                    paymentReference = paymentRef,
                                    providerName = null // Will be assigned by admin
                                )

                                if (result.isSuccess) {
                                    paymentStatus = "paid"
                                    showSuccessDialog = true
                                } else {
                                    errorMessage = "Payment failed. Please try again."
                                    showErrorDialog = true
                                }
                            } else {
                                errorMessage = "Payment request failed. Please check your mobile money balance and try again."
                                showErrorDialog = true
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = mobileMoneyProvider != null &&
                            mobileNumber.isNotEmpty() &&
                            mobileNumber.length >= 8 &&
                            !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💰", modifier = Modifier.size(24.dp)) // FIXED ICON
                            Text("Request Payment via Mobile Money", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Manual Payment Button (if user already paid)
                OutlinedButton(
                    onClick = {
                        paymentStatus = "manual"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Text("I've Already Paid (Enter Reference)")
                }

                // Cancel Button
                TextButton(
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Payment")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Manual Payment Dialog
    if (paymentStatus == "manual") {
        AlertDialog(
            onDismissRequest = { paymentStatus = null },
            title = { Text("📝 Manual Payment Reference") },
            text = {
                Column {
                    Text("If you've already paid via mobile money, please enter the payment reference from your SMS confirmation.")
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = paymentReference,
                        onValueChange = { paymentReference = it },
                        label = { Text("Payment Reference") },
                        placeholder = { Text("e.g., PAY123456789") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (paymentReference.isNotEmpty()) {
                            isLoading = true
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                val result = jobRepository.updateJobPayment(
                                    jobId = jobId,
                                    paymentStatus = "paid",
                                    transactionId = paymentRef,
                                    paymentReference = paymentReference,
                                    providerName = null
                                )
                                isLoading = false
                                if (result.isSuccess) {
                                    showSuccessDialog = true
                                    paymentStatus = null
                                } else {
                                    errorMessage = "Failed to verify payment. Please check reference and try again."
                                    showErrorDialog = true
                                }
                            }
                        }
                    },
                    enabled = paymentReference.isNotEmpty() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verify Payment")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentStatus = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🇸🇿", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎉 Payment Successful!")
                }
            },
            text = {
                Column {
                    Text("Your payment of $formattedAmount was successful.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Job ID: $jobId")
                    Text("Reference: $paymentRef")
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Next Steps:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "1. Admin will assign a qualified provider\n" +
                                        "2. Provider will contact you directly\n" +
                                        "3. Complete the job and leave a review",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        // Navigate to job details or dashboard
                        navController.navigate("job_details/$jobId") {
                            popUpTo("create_job") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Text("View Job Details")
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("⚠️ Payment Issue") },
            text = {
                Column {
                    Text(errorMessage)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Please try again or contact support if the issue persists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("Contact Support")
                }
            }
        )
    }
}

@Composable
fun InstructionStep(
    stepNumber: Int,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFF2E7D32), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stepNumber.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}