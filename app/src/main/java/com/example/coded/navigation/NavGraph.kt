package com.example.coded.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.coded.data.OasisAuthRepository
import com.example.coded.screens.SplashScreen
import com.example.coded.screens.MainEntryScreen
import com.example.coded.screens.LoginScreen
import com.example.coded.screens.client.CreateJobScreen
import com.example.coded.screens.admin.AdminDashboardScreen
import com.example.coded.screens.provider.SuggestedJobsScreen
import kotlin.random.Random

@Composable
fun OasisNavGraph(
    navController: NavHostController,
    authRepository: OasisAuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController = navController, authRepository = authRepository)
        }

        composable("login") {
            LoginScreen(navController = navController, authRepository = authRepository)
        }

        composable("main_entry") {
            MainEntryScreen(navController = navController, authRepository = authRepository)
        }

        // Guest/Client flow - CREATE JOB (no parameters needed)
        composable("client/create_job") {
            CreateJobScreen(navController)
        }

        // Guest home screen
        composable("guest/home") {
            GuestHomeScreen(navController)
        }

        // Provider flow
        composable("provider/home") {
            ProviderHomeScreen(navController)
        }

        composable("provider/suggested_jobs") {
            SuggestedJobsScreen(navController)
        }

        // Admin flow
        composable("admin/dashboard") {
            AdminDashboardScreen(navController)
        }

        // PAYMENT FLOW
        composable("pay_for_job/{tempTransactionId}/{tempJobId}/{totalPrice}") { backStackEntry ->
            PaymentScreen(
                navController = navController,
                tempTransactionId = backStackEntry.arguments?.getString("tempTransactionId") ?: "",
                tempJobId = backStackEntry.arguments?.getString("tempJobId") ?: "",
                totalPrice = backStackEntry.arguments?.getString("totalPrice") ?: "0.0"
            )
        }

        // JOB DETAILS - After successful payment
        composable("job_details/{finalTransactionId}/{finalJobId}") { backStackEntry ->
            JobDetailsScreen(
                navController = navController,
                finalTransactionId = backStackEntry.arguments?.getString("finalTransactionId") ?: "",
                finalJobId = backStackEntry.arguments?.getString("finalJobId") ?: ""
            )
        }
    }
}

@Composable
fun GuestHomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Welcome to Oasis! 🏝️",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Get services anonymously",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("client/create_job") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Service Request (Free to Post)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No account needed • Pay only when job is accepted",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ProviderHomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Welcome Provider! 👷",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate("provider/suggested_jobs") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Available Jobs")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun PaymentScreen(
    navController: NavHostController,
    tempTransactionId: String,
    tempJobId: String,
    totalPrice: String
) {
    var paymentMethod by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Complete Payment",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2E7D32)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Job will be activated after payment",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Order Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Temporary Job ID:", style = MaterialTheme.typography.bodyMedium)
                    Text(tempJobId.take(12) + "...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Amount:", style = MaterialTheme.typography.bodyMedium)
                    Text("E$totalPrice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Payment Methods
        Text("Select Payment Method:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        val paymentMethods = listOf(
            "Credit/Debit Card" to "💳",
            "Mobile Money" to "📱",
            "PayPal" to "🏦",
            "Cryptocurrency" to "₿"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            paymentMethods.forEach { (method, icon) ->
                Card(
                    onClick = { paymentMethod = method },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (paymentMethod == method) Color(0xFFE8F5E9) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(method, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Pay Button - FIXED the condition
        Button(
            onClick = {
                isProcessing = true
                // Simulate payment processing
                // After successful payment, generate FINAL transaction and job IDs
                android.os.Handler().postDelayed({
                    val finalTransactionId = "txn_final_${System.currentTimeMillis()}_${Random.nextInt(10000, 99999)}"
                    val finalJobId = "job_final_${System.currentTimeMillis()}_${Random.nextInt(10000, 99999)}"
                    isProcessing = false
                    navController.navigate("job_details/$finalTransactionId/$finalJobId")
                }, 1500)
            },
            enabled = paymentMethod.isNotEmpty() && !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32)
            )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Pay E$totalPrice", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Payment")
        }
    }
}

@Composable
fun JobDetailsScreen(
    navController: NavHostController,
    finalTransactionId: String,
    finalJobId: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✅", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Job Activated Successfully!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF2E7D32),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your job is now visible to providers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Official IDs
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transaction ID:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(finalTransactionId.take(10) + "...", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Job ID:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(finalJobId.take(10) + "...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Navigate back to main entry
                navController.popBackStack("main_entry", inclusive = false)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finish")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { navController.navigate("client/create_job") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Another Job")
        }
    }
}