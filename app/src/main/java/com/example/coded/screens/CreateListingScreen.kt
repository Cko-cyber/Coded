package com.example.coded.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.coded.data.AuthRepository
import com.example.coded.data.ListingTier
import com.example.coded.viewmodels.CreateListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    navController: NavController,
    authRepository: AuthRepository,
    viewModel: CreateListingViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()

    val userId = authRepository.getCurrentFirebaseUser()?.uid ?: ""

    var showTierDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = uiState.tier.maxImages)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    if (uiState.isSuccess) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Success!") },
            text = { Text("Your listing has been created successfully.") },
            confirmButton = {
                TextButton(onClick = {
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Listing") },
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
            // Title
            Text(
                text = "List Your Cattle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // User Token Info Card
            currentUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF013B33))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Tokens",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${user.token_balance} tokens",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Free Listings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${3 - user.free_listings_used} left this month",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Listing Tier Selection
            Text(
                text = "Select Listing Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Choose your listing priority and visibility",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTierDialog = true },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.tier.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF013B33)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (uiState.tier == ListingTier.FREE)
                                        "FREE"
                                    else
                                        "${getTierCost(uiState.tier)} tokens",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getTierDescription(uiState.tier),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowDown, "Change tier")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image Upload Section
            ImageUploadSection(
                selectedImages = uiState.selectedImages,
                maxImages = uiState.tier.maxImages,
                uploadProgress = uiState.uploadProgress,
                isLoading = uiState.isLoading,
                onAddImages = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveImage = { uri -> viewModel.removeImage(uri) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Form Fields
            FormFieldsSection(uiState = uiState, viewModel = viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            if (uiState.error != null) {
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
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Submit Button
            Button(
                onClick = {
                    if (userId.isNotEmpty()) {
                        viewModel.createListing(context, userId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF013B33)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating Listing...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Listing")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Tier Selection Dialog
    if (showTierDialog) {
        TierSelectionDialog(
            currentTier = uiState.tier,
            freeListingsLeft = 3 - (currentUser?.free_listings_used ?: 0),
            tokenBalance = currentUser?.token_balance ?: 0,
            onDismiss = { showTierDialog = false },
            onSelectTier = { tier ->
                viewModel.updateTier(tier)
                showTierDialog = false
            }
        )
    }
}

@Composable
fun TierSelectionDialog(
    currentTier: ListingTier,
    freeListingsLeft: Int,
    tokenBalance: Int,
    onDismiss: () -> Unit,
    onSelectTier: (ListingTier) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Choose Listing Type",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ListingTier.values().forEach { tier ->
                    val cost = getTierCost(tier)
                    val canAfford = if (tier == ListingTier.FREE) {
                        freeListingsLeft > 0
                    } else {
                        tokenBalance >= cost
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .then(
                                if (currentTier == tier) {
                                    Modifier.border(
                                        2.dp,
                                        Color(0xFF013B33),
                                        RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(enabled = canAfford) {
                                if (canAfford) {
                                    onSelectTier(tier)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!canAfford) Color.Gray.copy(alpha = 0.1f) else Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tier.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAfford) Color(0xFF013B33) else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (currentTier == tier) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            "Selected",
                                            tint = Color(0xFF013B33),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = getTierDescription(tier),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Max ${tier.maxImages} images",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            Surface(

                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (tier == ListingTier.FREE) {
                                        if (freeListingsLeft > 0) "$freeListingsLeft left" else "None left"
                                    } else {
                                        "$cost tokens"
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// REMOVE THE DUPLICATE FUNCTION DEFINITIONS BELOW THIS LINE
// Keep only one copy of each function:

fun getTierCost(tier: ListingTier): Int {
    return when (tier) {
        ListingTier.FREE -> 0
        ListingTier.BASIC -> 5
        ListingTier.BULK -> 10
        ListingTier.PREMIUM -> 20
    }
}

fun getTierColor(tier: ListingTier): Color {
    return when (tier) {
        ListingTier.FREE -> Color(0xFF4CAF50)
        ListingTier.BASIC -> Color(0xFF2196F3)
        ListingTier.BULK -> Color(0xFFFF6F00)
        ListingTier.PREMIUM -> Color(0xFFFFD700)
    }
}

fun getTierDescription(tier: ListingTier): String {
    return when (tier) {
        ListingTier.FREE -> "Basic visibility • Standard position"
        ListingTier.BASIC -> "Enhanced visibility • Priority in search"
        ListingTier.BULK -> "High visibility • Featured in category"
        ListingTier.PREMIUM -> "Maximum visibility • Top of all listings"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormFieldsSection(
    uiState: com.example.coded.viewmodels.CreateListingUiState,
    viewModel: CreateListingViewModel
) {
    val breedOptions = listOf("Angus", "Brahman", "Hereford", "Simmental", "Other")
    val dewormingOptions = listOf("None", "Partial", "Complete")
    val vaccinatedOptions = listOf("Yes", "No", "Unknown")

    var expandedBreed by remember { mutableStateOf(false) }
    var expandedDew by remember { mutableStateOf(false) }
    var expandedVac by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Breed Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedBreed,
                onExpandedChange = { expandedBreed = !expandedBreed }
            ) {
                OutlinedTextField(
                    value = uiState.breed,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Breed") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedBreed) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedBreed,
                    onDismissRequest = { expandedBreed = false }
                ) {
                    breedOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateBreed(option)
                                expandedBreed = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.age,
                onValueChange = { viewModel.updateAge(it) },
                label = { Text("Age (e.g., 15 months)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.price,
                onValueChange = { viewModel.updatePrice(it) },
                label = { Text("Price (E)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.location,
                onValueChange = { viewModel.updateLocation(it) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expandedDew,
                onExpandedChange = { expandedDew = !expandedDew }
            ) {
                OutlinedTextField(
                    value = uiState.deworming,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deworming") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDew) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedDew,
                    onDismissRequest = { expandedDew = false }
                ) {
                    dewormingOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateDeworming(option)
                                expandedDew = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expandedVac,
                onExpandedChange = { expandedVac = !expandedVac }
            ) {
                OutlinedTextField(
                    value = uiState.vaccinated,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Vaccinated") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedVac) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedVac,
                    onDismissRequest = { expandedVac = false }
                ) {
                    vaccinatedOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateVaccinated(option)
                                expandedVac = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.fullDetails,
                onValueChange = { viewModel.updateFullDetails(it) },
                label = { Text("Full Details") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

@Composable
fun ImageUploadSection(
    selectedImages: List<Uri>,
    maxImages: Int,
    uploadProgress: Map<Uri, Float>,
    isLoading: Boolean,
    onAddImages: () -> Unit,
    onRemoveImage: (Uri) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Images",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${selectedImages.size}/$maxImages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedImages) { uri ->
                        ImagePreviewItem(
                            uri = uri,
                            progress = uploadProgress[uri],
                            isLoading = isLoading,
                            onRemove = { onRemoveImage(uri) }
                        )
                    }

                    if (selectedImages.size < maxImages) {
                        item {
                            AddImageButton(onClick = onAddImages)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onAddImages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add Images")
                        Text(
                            text = "Up to $maxImages images",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedImages.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add at least one image of your cattle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ImagePreviewItem(
    uri: Uri,
    progress: Float?,
    isLoading: Boolean,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isLoading && progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(32.dp),
                    color = Color.White
                )
            }
        }

        if (!isLoading) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AddImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add Image",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}