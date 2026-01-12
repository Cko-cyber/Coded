package com.example.coded.screens.client

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.coded.data.*
import com.example.coded.ui.maps.GoogleMapAreaCalculator
import com.example.coded.ui.maps.GoogleMapLocationPicker
import com.example.coded.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.*
import com.example.coded.ui.theme.OasisGreen
import com.example.coded.ui.theme.OasisGreenDark
import com.example.coded.ui.theme.OasisGreenLight
import com.example.coded.ui.theme.OasisGold


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobRepository = remember { JobRepository(context) }
    val storageHelper = remember { SupabaseStorageHelper(context) }

    // State variables
    var serviceType by remember { mutableStateOf("") }
    var serviceVariant by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf<String?>(null) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var estimatedArea by remember { mutableStateOf<Double?>(null) }
    var vegetationType by remember { mutableStateOf("medium") }
    var growthStage by remember { mutableStateOf("medium") }
    var terrainType by remember { mutableStateOf("flat") }
    var needsDisposal by remember { mutableStateOf(false) }
    var isUrgent by remember { mutableStateOf(false) }
    var preferredDate by remember { mutableStateOf<Date?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mobileMoneyProvider by remember { mutableStateOf("mtn") }
    var mobileMoneyNumber by remember { mutableStateOf("") }

    // UI state
    var showLocationPicker by remember { mutableStateOf(false) }
    var showAreaCalculator by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isCreatingJob by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pricing
    var priceBreakdown by remember { mutableStateOf<JobPriceBreakdown?>(null) }

    // Calculate price whenever relevant fields change
    LaunchedEffect(
        serviceType, serviceVariant, estimatedArea, vegetationType,
        growthStage, terrainType, needsDisposal, isUrgent
    ) {
        if (serviceType.isNotEmpty()) {
            val config = JobPricingConfig()

            priceBreakdown = when (serviceType) {
                "grass_cutting", "yard_clearing", "gardening" -> {
                    if (estimatedArea != null && estimatedArea!! > 0) {
                        JobPricingCalculator.calculateAreaBasedPrice(
                            areaSqM = estimatedArea!!,
                            serviceType = serviceType,
                            vegetationType = vegetationType,
                            growthStage = growthStage,
                            terrainType = terrainType,
                            needsDisposal = needsDisposal && serviceType == "yard_clearing",
                            travelDistanceKm = 0.0,
                            isUrgent = isUrgent,
                            config = config
                        )
                    } else null
                }
                "tree_felling" -> {
                    JobPricingCalculator.calculateTreeFellingPrice(
                        treeSize = serviceVariant ?: "medium_tree",
                        treeHeight = 10.0,
                        locationComplexity = "normal",
                        needsStumpRemoval = false,
                        needsCleanup = true,
                        travelDistanceKm = 0.0,
                        config = config
                    )
                }
                else -> {
                    JobPricingCalculator.calculateServicePrice(
                        serviceType = serviceType,
                        serviceVariant = serviceVariant,
                        isUrgent = isUrgent,
                        travelDistanceKm = 0.0,
                        config = config
                    )
                }
            }
        }
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty() && selectedImages.size + uris.size <= 3) {
            selectedImages = selectedImages + uris
        }
    }

    // Service types
    val serviceTypes = listOf(
        "grass_cutting" to "🌱 Grass Cutting",
        "yard_clearing" to "🏡 Yard Clearing",
        "gardening" to "🌻 Gardening",
        "tree_felling" to "🌳 Tree Felling",
        "cleaning" to "🧹 Cleaning",
        "plumbing" to "🔧 Plumbing",
        "electrical" to "⚡ Electrical",
        "dstv_installation" to "📡 DSTV Installation",
        "maintenance" to "🔨 Maintenance"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Service Job",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OasisGreen
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, "Error", tint = Color(0xFFD32F2F))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                // Service Type Selection
                ServiceTypeSection(
                    serviceType = serviceType,
                    onServiceTypeChange = {
                        serviceType = it
                        serviceVariant = null // Reset variant
                    },
                    serviceTypes = serviceTypes
                )

                // Service Variant (for specific services)
                if (serviceType in listOf("plumbing", "electrical", "dstv_installation", "tree_felling")) {
                    ServiceVariantSection(
                        serviceType = serviceType,
                        selectedVariant = serviceVariant,
                        onVariantChange = { serviceVariant = it }
                    )
                }

                // Location Picker
                LocationSection(
                    locationAddress = locationAddress,
                    onPickLocation = { showLocationPicker = true }
                )

                // Area Calculator (for area-based services)
                if (serviceType in listOf("grass_cutting", "yard_clearing", "gardening")) {
                    AreaCalculatorSection(
                        estimatedArea = estimatedArea,
                        onCalculateArea = { showAreaCalculator = true },
                        vegetationType = vegetationType,
                        onVegetationTypeChange = { vegetationType = it },
                        growthStage = growthStage,
                        onGrowthStageChange = { growthStage = it },
                        terrainType = terrainType,
                        onTerrainTypeChange = { terrainType = it },
                        needsDisposal = needsDisposal && serviceType == "yard_clearing",
                        onNeedsDisposalChange = { needsDisposal = it },
                        showDisposal = serviceType == "yard_clearing"
                    )
                }

                // Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Job Description *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OasisGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                if (it.length <= 500) description = it
                            },
                            placeholder = { Text("Describe the work needed in detail...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OasisMint,
                                focusedLabelColor = OasisGreen,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${description.length}/500 characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = OasisGray,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // Image Upload
                ImageUploadSection(
                    selectedImages = selectedImages,
                    onAddImages = { imagePickerLauncher.launch("image/*") },
                    onRemoveImage = { uri ->
                        selectedImages = selectedImages.filter { it != uri }
                    }
                )

                // Urgency & Date
                UrgencySection(
                    isUrgent = isUrgent,
                    onUrgentChange = { isUrgent = it },
                    preferredDate = preferredDate,
                    onPickDate = { showDatePicker = true }
                )

                // Mobile Money Details
                MobileMoneySection(
                    provider = mobileMoneyProvider,
                    onProviderChange = { mobileMoneyProvider = it },
                    number = mobileMoneyNumber,
                    onNumberChange = { mobileMoneyNumber = it }
                )

                // Price Breakdown
                priceBreakdown?.let { breakdown ->
                    PriceBreakdownCard(breakdown = breakdown)
                }

                // Create Job Button
                Button(
                    onClick = {
                        // Validate
                        when {
                            serviceType.isEmpty() -> errorMessage = "Please select a service type"
                            description.isEmpty() -> errorMessage = "Please enter a description"
                            locationAddress == null -> errorMessage = "Please select a location"
                            serviceType in listOf("grass_cutting", "yard_clearing", "gardening") && estimatedArea == null ->
                                errorMessage = "Please calculate the area"
                            priceBreakdown == null -> errorMessage = "Price calculation error"
                            mobileMoneyNumber.length < 8 -> errorMessage = "Please enter a valid mobile number"
                            else -> {
                                errorMessage = null
                                isCreatingJob = true

                                scope.launch {
                                    try {
                                        // Create job in Firebase
                                        val result = jobRepository.createJob(
                                            serviceType = serviceType,
                                            description = description,
                                            locationAddress = locationAddress!!,
                                            locationLat = locationLat,
                                            locationLng = locationLng,
                                            estimatedArea = estimatedArea,
                                            vegetationType = if (serviceType in listOf("grass_cutting", "yard_clearing", "gardening")) vegetationType else null,
                                            growthStage = if (serviceType in listOf("grass_cutting", "yard_clearing", "gardening")) growthStage else null,
                                            terrainType = if (serviceType in listOf("grass_cutting", "yard_clearing", "gardening")) terrainType else null,
                                            serviceVariant = serviceVariant,
                                            priceBreakdown = priceBreakdown!!,
                                            imageUris = selectedImages,
                                            mobileMoneyProvider = mobileMoneyProvider,
                                            mobileMoneyNumber = mobileMoneyNumber,
                                            needsDisposal = needsDisposal,
                                            isUrgent = isUrgent,
                                            preferredDate = preferredDate?.let
                                            { Timestamp(it) }
                                        )

                                        if (result.isSuccess) {
                                            val jobId = result.getOrNull()!!
                                            // Navigate to payment confirmation
                                            navController.navigate("payment_confirmation/$jobId/${priceBreakdown!!.totalAmount}")
                                        } else {
                                            errorMessage = "Failed to create job: ${result.exceptionOrNull()?.message}"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                    } finally {
                                        isCreatingJob = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isCreatingJob && priceBreakdown != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OasisGreen,
                        disabledContainerColor = OasisGray.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    if (isCreatingJob) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowForward, "Continue", tint = Color.White)
                            Text(
                                "Continue to Payment ${priceBreakdown?.formattedTotal ?: ""}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "By creating this job, you agree to our Terms of Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = OasisGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Location Picker Dialog
    if (showLocationPicker) {
        GoogleMapLocationPicker(
            onLocationSelected = { latLng, address ->
                locationLat = latLng.latitude
                locationLng = latLng.longitude
                locationAddress = address
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false }
        )
    }

    // Area Calculator Dialog
    if (showAreaCalculator && locationLat != null && locationLng != null) {
        GoogleMapAreaCalculator(
            location = LatLng(locationLat!!, locationLng!!),
            onAreaCalculated = { area ->
                estimatedArea = area
                showAreaCalculator = false
            },
            onDismiss = { showAreaCalculator = false }
        )
    }
}

@Composable
private fun ServiceTypeSection(
    serviceType: String,
    onServiceTypeChange: (String) -> Unit,
    serviceTypes: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Service Type *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OasisGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                serviceTypes.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { (id, label) ->
                            FilterChip(
                                selected = serviceType == id,
                                onClick = { onServiceTypeChange(id) },
                                label = {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OasisGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF5F5F5)
                                ),

                                modifier = Modifier.weight(1f),
                                enabled = true
                            )
                        }
                        // Add empty weight to fill the row if needed
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceVariantSection(
    serviceType: String,
    selectedVariant: String?,
    onVariantChange: (String) -> Unit
) {
    val variants = when (serviceType) {
        "plumbing" -> listOf(
            "leaking_tap" to "🚰 Leaking Tap",
            "blocked_drain" to "🚽 Blocked Drain",
            "toilet_repair" to "🚿 Toilet Repair",
            "pipe_fixing" to "🔧 Pipe Fixing",
            "water_heater" to "♨️ Water Heater"
        )
        "electrical" -> listOf(
            "socket_repair" to "🔌 Socket Repair",
            "light_installation" to "💡 Light Installation",
            "switch_fixing" to "🎚️ Switch Fixing",
            "wiring" to "⚡ Wiring",
            "circuit_breaker" to "🔋 Circuit Breaker"
        )
        "dstv_installation" -> listOf(
            "standard" to "📺 Standard",
            "extra_large" to "📺 Extra Large",
            "dual_view" to "📺 Dual View",
            "multi_room" to "📺 Multi-room"
        )
        "tree_felling" -> listOf(
            "small_tree" to "🌱 Small Tree (<30cm)",
            "medium_tree" to "🌳 Medium Tree (30-60cm)",
            "large_tree" to "🌲 Large Tree (>60cm)",
            "palm_tree" to "🌴 Palm Tree",
            "fruit_tree" to "🍎 Fruit Tree"
        )
        else -> emptyList()
    }

    if (variants.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            border = BorderStroke(1.dp, OasisMint.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Variant *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OasisDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    variants.forEach { (id, label) ->
                        FilterChip(
                            selected = selectedVariant == id,
                            onClick = { onVariantChange(id) },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OasisGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    locationAddress: String?,
    onPickLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Job Location *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OasisGreen
                )
                Icon(
                    Icons.Default.LocationOn,
                    "Location",
                    tint = OasisMint
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPickLocation,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = OasisGreen
                ),
                border = BorderStroke(1.dp, OasisMint)
            ) {
                Icon(Icons.Default.LocationOn, "Map")
                Spacer(modifier = Modifier.width(8.dp))
                Text(locationAddress ?: "Pick Location on Map")
            }
            if (locationAddress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = OasisGreen.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, OasisMint.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Selected",
                            tint = OasisGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            locationAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = OasisDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AreaCalculatorSection(
    estimatedArea: Double?,
    onCalculateArea: () -> Unit,
    vegetationType: String,
    onVegetationTypeChange: (String) -> Unit,
    growthStage: String,
    onGrowthStageChange: (String) -> Unit,
    terrainType: String,
    onTerrainTypeChange: (String) -> Unit,
    needsDisposal: Boolean,
    onNeedsDisposalChange: (Boolean) -> Unit,
    showDisposal: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Area & Conditions *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OasisGreen
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCalculateArea,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = OasisGreen
                ),
                border = BorderStroke(1.dp, OasisMint)
            ) {
                Text(
                    estimatedArea?.let { "📐 Area: ${it.toInt()} m²" } ?: "📐 Calculate Area on Map",
                    fontWeight = FontWeight.Medium
                )
            }

            if (estimatedArea != null) {
                Spacer(modifier = Modifier.height(16.dp))

                // Vegetation Type
                Text("Vegetation Type:", style = MaterialTheme.typography.labelMedium, color = OasisDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("light" to "Light", "medium" to "Medium", "heavy" to "Heavy", "overgrown" to "Overgrown").forEach { (id, label) ->
                        FilterChip(
                            selected = vegetationType == id,
                            onClick = { onVegetationTypeChange(id) },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OasisGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Growth Stage
                Text("Growth Stage:", style = MaterialTheme.typography.labelMedium, color = OasisDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("new" to "New", "medium" to "Medium", "mature" to "Mature").forEach { (id, label) ->
                        FilterChip(
                            selected = growthStage == id,
                            onClick = { onGrowthStageChange(id) },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OasisGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Terrain Type
                Text("Terrain Type:", style = MaterialTheme.typography.labelMedium, color = OasisDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("flat" to "Flat", "sloped" to "Sloped", "uneven" to "Uneven").forEach { (id, label) ->
                        FilterChip(
                            selected = terrainType == id,
                            onClick = { onTerrainTypeChange(id) },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OasisGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = true
                        )
                    }
                }

                if (showDisposal) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OasisGreen.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, OasisMint.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Needs Disposal?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Extra cost applies", style = MaterialTheme.typography.bodySmall, color = OasisGray)
                            }
                            Switch(
                                checked = needsDisposal,
                                onCheckedChange = onNeedsDisposalChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OasisGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = OasisGray
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageUploadSection(
    selectedImages: List<Uri>,
    onAddImages: () -> Unit,
    onRemoveImage: (Uri) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reference Photos (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OasisGreen
                )
                Text(
                    "${selectedImages.size}/3",
                    style = MaterialTheme.typography.bodySmall,
                    color = OasisGray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Upload photos to help workers understand the job better",
                style = MaterialTheme.typography.bodySmall,
                color = OasisGray
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (selectedImages.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedImages.forEach { uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .weight(1f)
                        ) {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = "Reference image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            IconButton(
                                onClick = { onRemoveImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(4.dp, (-4).dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF5350)),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Close,
                                            "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (selectedImages.size < 3) {
                OutlinedButton(
                    onClick = onAddImages,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = OasisGreen
                    ),
                    border = BorderStroke(1.dp, OasisMint)
                ) {

                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Photos")
                }
            }
        }
    }
}

@Composable
private fun UrgencySection(
    isUrgent: Boolean,
    onUrgentChange: (Boolean) -> Unit,
    preferredDate: Date?,
    onPickDate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isUrgent) OasisMint.copy(alpha = 0.5f) else Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🚨 Urgent Job", fontWeight = FontWeight.Bold, color = OasisDark)
                    Text(
                        "Add E200 for priority assignment & faster response",
                        style = MaterialTheme.typography.bodySmall,
                        color = OasisGray
                    )
                }
                Switch(
                    checked = isUrgent,
                    onCheckedChange = onUrgentChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OasisGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = OasisGray
                    )
                )
            }
        }
    }
}

@Composable
private fun MobileMoneySection(
    provider: String,
    onProviderChange: (String) -> Unit,
    number: String,
    onNumberChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📱 Mobile Money Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OasisGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Your payment will be processed via mobile money",
                style = MaterialTheme.typography.bodySmall,
                color = OasisGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Provider *", style = MaterialTheme.typography.labelMedium, color = OasisDark)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = provider == "mtn",
                    onClick = { onProviderChange("mtn") },
                    label = { Text("MTN", fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OasisGreen,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = true
                )
                FilterChip(
                    selected = provider == "eswatini_mobile",
                    onClick = { onProviderChange("eswatini_mobile") },
                    label = { Text("Eswatini Mobile", fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OasisGreen,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = number,
                onValueChange = {
                    if (it.all { char -> char.isDigit() || char.isWhitespace() })
                        onNumberChange(it)
                },
                label = { Text("Mobile Number *") },
                placeholder = { Text("7612 3456") },
                leadingIcon = { Icon(Icons.Default.Phone, "Phone", tint = OasisMint) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OasisMint,
                    focusedLabelColor = OasisGreen,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "This number will be used for payment confirmation",
                style = MaterialTheme.typography.bodySmall,
                color = OasisGray
            )
        }
    }
}

@Composable
private fun PriceBreakdownCard(breakdown: JobPriceBreakdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = OasisGreen.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, OasisMint.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "💰 Price Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OasisGreen
                )
                Text(
                    breakdown.formattedEstimatedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = OasisGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = OasisMint.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            breakdown.getDetailedBreakdown().forEach { (label, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = if (label.contains("TOTAL"))
                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.bodyMedium,
                        color = if (label.contains("TOTAL")) OasisGreen else OasisDark
                    )
                    Text(
                        amount,
                        style = if (label.contains("TOTAL"))
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.bodyMedium,
                        color = if (label.contains("TOTAL")) OasisGreen else OasisDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = OasisMint.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "💡 Price includes labor, basic equipment, and local travel",
                style = MaterialTheme.typography.bodySmall,
                color = OasisGray
            )
        }
    }
}