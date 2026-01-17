package com.example.coded.navigation

import androidx.compose.foundation.background
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
import com.example.coded.ui.theme.OasisGreen
import com.example.coded.ui.theme.OasisGreenDark
import com.example.coded.ui.theme.OasisGreenLight
import com.example.coded.ui.theme.OasisGold
import kotlin.random.Random
import androidx.compose.foundation.BorderStroke

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

        composable("client/create_job") {
            CreateJobScreen(navController)
        }

        composable("guest/home") {
            GuestHomeScreen(navController)
        }

        composable("provider/home") {
            ProviderHomeScreen(navController)
        }

        composable("provider/suggested_jobs") {
            SuggestedJobsScreen(navController)
        }

        composable("admin/dashboard") {
            AdminDashboardScreen(navController)
        }

        composable("pay_for_job/{tempTransactionId}/{tempJobId}/{totalPrice}") { backStackEntry ->
            PaymentScreen(
                navController = navController,
                tempTransactionId = backStackEntry.arguments?.getString("tempTransactionId") ?: "",
                tempJobId = backStackEntry.arguments?.getString("tempJobId") ?: "",
                totalPrice = backStackEntry.arguments?.getString("totalPrice") ?: "0.0"
            )
        }

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OasisGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to Oasis! 🏝️",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Get services anonymously",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { navController.navigate("client/create_job") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OasisGold,
                    contentColor = OasisGreenDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    "Create Service Request (Free to Post)",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "No account needed • Pay only when job is accepted",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ProviderHomeScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OasisGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome Provider! 👷",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { navController.navigate("provider/suggested_jobs") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OasisGold,
                    contentColor = OasisGreenDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("View Available Jobs", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            ) {
                Text("Logout", fontWeight = FontWeight.Bold)
            }
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
            .background(OasisGreenLight)
            .padding(16.dp)
    ) {
        Text(
            "Complete Payment",
            style = MaterialTheme.typography.headlineMedium,
            color = OasisGreenDark,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Job will be activated after payment",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Order Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Temporary Job ID:", style = MaterialTheme.typography.bodyLarge)
                    Text(tempJobId.take(12) + "...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Amount:", style = MaterialTheme.typography.bodyLarge)
                    Text("E$totalPrice", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OasisGreenDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Select Payment Method:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OasisGreenDark)
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
                        containerColor = if (paymentMethod == method) OasisGreenLight else Color.White
                    ),
                    border = if (paymentMethod == method)
                        CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(OasisGreen)
                        )
                    else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(method, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isProcessing = true
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
                containerColor = OasisGreen
            )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Pay E$totalPrice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Payment", color = OasisGreenDark)
        }
    }
}

@Composable
fun JobDetailsScreen(
    navController: NavHostController,
    finalTransactionId: String,
    finalJobId: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OasisGreen)
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
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✅", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Job Activated Successfully!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OasisGreenDark,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your job is now visible to providers",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = OasisGreenLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Transaction ID:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Text(finalTransactionId.take(10) + "...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Job ID:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Text(finalJobId.take(10) + "...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    navController.popBackStack("main_entry", inclusive = false)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OasisGold,
                    contentColor = OasisGreenDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Finish", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.navigate("client/create_job") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            ) {
                Text("Create Another Job", fontWeight = FontWeight.Bold)
            }
        }
    }
}