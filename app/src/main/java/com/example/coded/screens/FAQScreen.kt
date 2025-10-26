package com.example.coded.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

// ==================== FAQ SCREEN ====================

data class FAQItem(
    val question: String,
    val answer: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(navController: NavController) {
    val faqs = listOf(
        FAQItem(
            "How do I create a listing?",
            "Navigate to the Create tab in the bottom navigation, select your images, fill in the details about your livestock, and submit. You get 3 free listings per month!",
            "Listings"
        ),
        FAQItem(
            "What are tokens used for?",
            "Tokens allow you to create premium listings with more photos, better visibility, and advanced features. You can purchase token packages from your profile.",
            "Tokens"
        ),
        FAQItem(
            "How do I contact a seller?",
            "Open any listing and tap the 'Contact Seller' button. This will open a direct chat with the seller where you can discuss the livestock.",
            "Messaging"
        ),
        FAQItem(
            "What listing tiers are available?",
            "We offer 4 tiers:\n• Free (3 photos)\n• Basic (6 photos, 2 tokens)\n• Bulk (6 photos for multiple listings, 5 tokens)\n• Premium (6 photos, featured placement, 10 tokens)",
            "Listings"
        ),
        FAQItem(
            "How do I edit my listing?",
            "Go to Profile → My Listings, find your listing, and tap the Edit button. You can update all details except images (delete and create new to change images).",
            "Listings"
        ),
        FAQItem(
            "Is my payment information secure?",
            "Yes! All payments are processed securely. We use industry-standard encryption and never store your payment details on our servers.",
            "Payments"
        ),
        FAQItem(
            "How do I reset my password?",
            "On the login screen, tap 'Forgot Password?' and follow the instructions. You'll receive an email with a reset link.",
            "Account"
        ),
        FAQItem(
            "Can I deactivate a listing temporarily?",
            "Yes! In My Listings, use the toggle button to deactivate/activate listings without deleting them.",
            "Listings"
        ),
        FAQItem(
            "What payment methods do you accept?",
            "We accept mobile money (MTN, Swazi Mobile), bank cards, and bank transfers for token purchases.",
            "Payments"
        ),
        FAQItem(
            "How do I report inappropriate content?",
            "Tap the menu icon on any listing and select 'Report'. Our team reviews all reports within 24 hours.",
            "Safety"
        )
    )

    var expandedItem by remember { mutableStateOf<FAQItem?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All") + faqs.map { it.category }.distinct()
    val filteredFAQs = if (selectedCategory == "All") {
        faqs
    } else {
        faqs.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frequently Asked Questions") },
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
        ) {
            // Category Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF013B33),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // FAQ Items
            filteredFAQs.forEach { faq ->
                FAQItemCard(
                    faq = faq,
                    isExpanded = expandedItem == faq,
                    onToggle = {
                        expandedItem = if (expandedItem == faq) null else faq
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Still have questions card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF013B33).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ContactSupport,
                        contentDescription = null,
                        tint = Color(0xFF013B33),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Still have questions?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF013B33)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("contact_support") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF013B33)
                        )
                    ) {
                        Text("Contact Support")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FAQItemCard(
    faq: FAQItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF013B33)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ==================== CONTACT SUPPORT SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSupportScreen(navController: NavController, authRepository: AuthRepository) {
    val currentUser by authRepository.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General Inquiry") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "General Inquiry",
        "Technical Issue",
        "Payment Problem",
        "Account Help",
        "Report Listing",
        "Feature Request",
        "Other"
    )

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Message Sent!") },
            text = {
                Text("Our support team will get back to you within 24 hours via email.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF013B33)
                    )
                ) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Support") },
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
                text = "How can we help you?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF013B33)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fill out the form below and our team will respond within 24 hours.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Contact Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickContactCard(
                    icon = Icons.Default.Email,
                    title = "Email",
                    value = "support@herdmat.co.sz",
                    modifier = Modifier.weight(1f)
                )
                QuickContactCard(
                    icon = Icons.Default.Phone,
                    title = "Phone",
                    value = "+268 7612 3456",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Dropdown
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF013B33),
                        focusedLabelColor = Color(0xFF013B33)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subject
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF013B33),
                    focusedLabelColor = Color(0xFF013B33)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Message
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                minLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF013B33),
                    focusedLabelColor = Color(0xFF013B33)
                )
            )

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

            // Submit Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        when {
                            subject.isBlank() -> errorMessage = "Please enter a subject"
                            message.isBlank() -> errorMessage = "Please enter a message"
                            else -> {
                                isSubmitting = true
                                errorMessage = null

                                // Simulate API call
                                kotlinx.coroutines.delay(2000)

                                // In production, send to your backend
                                isSubmitting = false
                                showSuccessDialog = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                ),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending...")
                } else {
                    Icon(Icons.Default.Send, "Send")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Message")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun QuickContactCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF013B33).copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF013B33),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF013B33)
            )
        }
    }
}