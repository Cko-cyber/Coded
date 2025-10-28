package com.example.coded.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coded.data.AuthRepository
import kotlinx.coroutines.launch

// Token packages
data class TokenPackage(
    val tokens: Int,
    val price: Double,
    val popular: Boolean = false,
    val bonus: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyTokensScreen(navController: NavController, authRepository: AuthRepository) {
    val currentUser by authRepository.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedPackage by remember { mutableStateOf<TokenPackage?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Token packages
    val tokenPackages = listOf(
        TokenPackage(tokens = 10, price = 50.0),
        TokenPackage(tokens = 25, price = 100.0, bonus = 5),
        TokenPackage(tokens = 50, price = 180.0, popular = true, bonus = 15),
        TokenPackage(tokens = 100, price = 300.0, bonus = 35),
        TokenPackage(tokens = 250, price = 650.0, bonus = 100)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buy Tokens") },
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
            // Current Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF013B33)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Token Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${currentUser?.token_balance ?: 0}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "tokens",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF6F00)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "What are tokens used for?",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tokens unlock premium listing features for better visibility and priority placement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Choose a Package",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Token Packages
            tokenPackages.forEach { package_ ->
                TokenPackageCard(
                    package_ = package_,
                    isSelected = selectedPackage == package_,
                    onClick = { selectedPackage = package_ }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Purchase Button
            Button(
                onClick = {
                    if (selectedPackage != null) {
                        showPaymentDialog = true
                    } else {
                        errorMessage = "Please select a token package"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                ),
                enabled = !isProcessing && selectedPackage != null
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Default.ShoppingCart, "Buy")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue to Payment")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Info
            Text(
                text = "Payment via MTN MoMo • Safe & Secure",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // MoMo Payment Dialog
    if (showPaymentDialog && selectedPackage != null) {
        MoMoPaymentDialog(
            package_ = selectedPackage!!,
            phoneNumber = phoneNumber,
            onPhoneNumberChange = { phoneNumber = it },
            onDismiss = { showPaymentDialog = false },
            onConfirm = {
                coroutineScope.launch {
                    isProcessing = true
                    errorMessage = null

                    // TODO: Integrate MoMo Pay API here
                    val success = processMoMoPayment(
                        userId = currentUser?.id ?: "",
                        phoneNumber = phoneNumber,
                        amount = selectedPackage!!.price,
                        tokens = selectedPackage!!.tokens + selectedPackage!!.bonus
                    )

                    isProcessing = false

                    if (success) {
                        showPaymentDialog = false
                        // Refresh user data to show new token balance
                        authRepository.refreshUserData()
                        navController.popBackStack()
                    } else {
                        errorMessage = "Payment failed. Please try again."
                    }
                }
            }
        )
    }
}

@Composable
fun TokenPackageCard(
    package_: TokenPackage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        3.dp,
                        Color(0xFF013B33),
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (package_.popular) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (package_.popular) Color(0xFFFFF8E1) else Color.White
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Popular Badge
            if (package_.popular) {
                Surface(
                    color = Color(0xFFFF6F00),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "MOST POPULAR",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(top = if (package_.popular) 24.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${package_.tokens}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF013B33)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "tokens",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray
                            )
                        }

                        if (package_.bonus > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFF4CAF50),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "+${package_.bonus} BONUS",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "E ${package_.price}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )
                        Text(
                            text = "E ${String.format("%.2f", package_.price / (package_.tokens + package_.bonus))} per token",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Selected",
                            tint = Color(0xFF013B33),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Selected",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF013B33)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoMoPaymentDialog(
    package_: TokenPackage,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    tint = Color(0xFF013B33)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("MTN MoMo Payment", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                // Package Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tokens:")
                            Text(
                                "${package_.tokens + package_.bonus}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (package_.bonus > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bonus:", color = Color(0xFF4CAF50))
                                Text(
                                    "+${package_.bonus}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total:", fontWeight = FontWeight.Bold)
                            Text(
                                "E ${package_.price}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF013B33),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        val cleaned = newValue.filter { it.isDigit() }
                        if (cleaned.length <= 8) {
                            onPhoneNumberChange(cleaned)
                        }
                    },
                    label = { Text("MTN MoMo Number") },
                    placeholder = { Text("76123456") },
                    leadingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "+268",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Phone, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Instructions
                Text(
                    text = "You will receive a prompt on your phone to authorize the payment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = phoneNumber.length == 8,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                )
            ) {
                Text("Pay E ${package_.price}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// TODO: Integrate actual MoMo Pay API
suspend fun processMoMoPayment(
    userId: String,
    phoneNumber: String,
    amount: Double,
    tokens: Int
): Boolean {
    // This is a placeholder - integrate with actual MoMo Pay API
    // Reference: MTN MoMo API documentation for Eswatini

    return try {
        // Steps for MoMo integration:
        // 1. Generate transaction ID
        // 2. Call MoMo /collection/v1_0/requesttopay endpoint
        // 3. Poll for payment status
        // 4. If successful, credit tokens to user
        // 5. Create transaction record

        // For now, simulate delay
        kotlinx.coroutines.delay(2000)

        // Return true for testing (replace with actual API call result)
        false // Change to true after API integration
    } catch (e: Exception) {
        false
    }
}