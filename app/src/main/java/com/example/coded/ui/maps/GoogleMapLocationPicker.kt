package com.example.coded.ui.maps

import android.Manifest
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.util.*

// Eswatini coordinates
object EswatiniLocations {
    // Major urban and semi-urban areas in Eswatini
    val MBABANE = LatLng(-26.305448, 31.136672)  // Capital city center
    val MANZINI = LatLng(-26.498837, 31.380817)  // Commercial hub center
    val MATSAPHA = LatLng(-26.513056, 31.308889) // Industrial area
    val SIDVOKODVO = LatLng(-26.616667, 31.416667) // Semi-urban area
    val NHLANGANO = LatLng(-27.112222, 31.198333) // Southern town

    // Default center point (geometric center of main areas)
    val DEFAULT_CENTER = LatLng(-26.482000, 31.305000) // Central point between main areas

    // Common service area boundaries
    val SERVICE_AREA_BOUNDS = LatLngBounds(
        LatLng(-26.650000, 31.100000), // Southwest bound
        LatLng(-26.300000, 31.450000)  // Northeast bound
    )

    // Default search suggestions for Eswatini
    val DEFAULT_SUGGESTIONS = listOf(
        "Mbabane, Eswatini",
        "Manzini, Eswatini",
        "Matsapha, Eswatini",
        "Sidvokodvo, Eswatini",
        "Nhlangano, Eswatini"
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapLocationPicker(
    onLocationSelected: (LatLng, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(EswatiniLocations.DEFAULT_CENTER, 12f)
    }

    // Location permissions
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Check permissions
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Job Location - Eswatini", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Location Buttons
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            "Quick Select:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    selectedLocation = EswatiniLocations.MBABANE
                                    getAddressFromLatLng(EswatiniLocations.MBABANE, context) { address ->
                                        selectedAddress = address
                                    }
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                EswatiniLocations.MBABANE,
                                                15f
                                            )
                                        )
                                    }
                                },
                                label = { Text("Mbabane") },
                                modifier = Modifier.height(32.dp)
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    selectedLocation = EswatiniLocations.MANZINI
                                    getAddressFromLatLng(EswatiniLocations.MANZINI, context) { address ->
                                        selectedAddress = address
                                    }
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                EswatiniLocations.MANZINI,
                                                15f
                                            )
                                        )
                                    }
                                },
                                label = { Text("Manzini") },
                                modifier = Modifier.height(32.dp)
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    selectedLocation = EswatiniLocations.MATSAPHA
                                    getAddressFromLatLng(EswatiniLocations.MATSAPHA, context) { address ->
                                        selectedAddress = address
                                    }
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                EswatiniLocations.MATSAPHA,
                                                15f
                                            )
                                        )
                                    }
                                },
                                label = { Text("Matsapha") },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                // Search Bar
                LocationSearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        showSuggestions = it.isNotEmpty()
                    },
                    onSearch = { query ->
                        performSearch(query, context) { latLng, address ->
                            selectedLocation = latLng
                            selectedAddress = address
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                )
                            }
                            showSuggestions = false
                        }
                    }
                )

                // Search Suggestions
                if (showSuggestions) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            EswatiniLocations.DEFAULT_SUGGESTIONS.forEach { suggestion ->
                                TextButton(
                                    onClick = {
                                        searchQuery = suggestion
                                        performSearch(suggestion, context) { latLng, address ->
                                            selectedLocation = latLng
                                            selectedAddress = address
                                            coroutineScope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                                )
                                            }
                                            showSuggestions = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        suggestion,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Map
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            mapType = MapType.NORMAL,
                            isMyLocationEnabled = locationPermissionsState.allPermissionsGranted,
                            latLngBoundsForCameraTarget = EswatiniLocations.SERVICE_AREA_BOUNDS
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            compassEnabled = true,
                            myLocationButtonEnabled = locationPermissionsState.allPermissionsGranted,
                            mapToolbarEnabled = false
                        ),
                        onMapClick = { latLng ->
                            selectedLocation = latLng
                            getAddressFromLatLng(latLng, context) { address ->
                                selectedAddress = address
                            }
                        }
                    ) {
                        // Selected Marker
                        selectedLocation?.let { latLng ->
                            Marker(
                                state = MarkerState(position = latLng),
                                title = "Job Location",
                                snippet = selectedAddress,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }

                        // Major town markers
                        Marker(
                            state = MarkerState(position = EswatiniLocations.MBABANE),
                            title = "Mbabane",
                            snippet = "Capital City",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        )
                        Marker(
                            state = MarkerState(position = EswatiniLocations.MANZINI),
                            title = "Manzini",
                            snippet = "Commercial Hub",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                        )
                        Marker(
                            state = MarkerState(position = EswatiniLocations.MATSAPHA),
                            title = "Matsapha",
                            snippet = "Industrial Area",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        )
                    }

                    // Center Marker Icon (when no location selected)
                    if (selectedLocation == null) {
                        Icon(
                            Icons.Default.LocationOn,
                            "Center Marker",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Selected Location Info
                if (selectedLocation != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Selected Location",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                selectedAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Lat: ${selectedLocation?.latitude?.format(6)}, Lng: ${selectedLocation?.longitude?.format(6)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                "📍 Eswatini",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, "Info", tint = Color(0xFFF57C00))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tap on the map or search to select location",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedLocation?.let { latLng ->
                        onLocationSelected(latLng, selectedAddress)
                    }
                },
                enabled = selectedLocation != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, "Confirm")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select This Location")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun Double.format(decimals: Int): String = String.format("%.${decimals}f", this)

private fun getAddressFromLatLng(latLng: LatLng, context: android.content.Context, onResult: (String) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (addresses?.isNotEmpty() == true) {
            val address = addresses[0]
            val addressText = buildString {
                // Start with Eswatini-specific addressing
                address.thoroughfare?.let { append(it) }
                address.subLocality?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
                address.locality?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
                address.adminArea?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
                if (isEmpty()) {
                    append("Location near Eswatini (${latLng.latitude.format(4)}, ${latLng.longitude.format(4)})")
                } else {
                    append(", Eswatini")
                }
            }
            onResult(addressText)
        } else {
            onResult("Location in Eswatini (${latLng.latitude.format(4)}, ${latLng.longitude.format(4)})")
        }
    } catch (e: Exception) {
        onResult("Location in Eswatini (${latLng.latitude.format(4)}, ${latLng.longitude.format(4)})")
    }
}

private fun performSearch(query: String, context: android.content.Context, onResult: (LatLng, String) -> Unit) {
    if (query.isEmpty()) return

    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocationName("$query, Eswatini", 1)
        if (addresses?.isNotEmpty() == true) {
            val address = addresses[0]
            val latLng = LatLng(address.latitude, address.longitude)
            val addressText = buildString {
                address.thoroughfare?.let { append(it) }
                address.subLocality?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
                address.locality?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
                if (isEmpty()) {
                    append("$query, Eswatini")
                } else {
                    append(", Eswatini")
                }
            }
            onResult(latLng, addressText)
        } else {
            // Fallback to general search
            @Suppress("DEPRECATION")
            val fallbackAddresses = geocoder.getFromLocationName(query, 1)
            if (fallbackAddresses?.isNotEmpty() == true) {
                val address = fallbackAddresses[0]
                val latLng = LatLng(address.latitude, address.longitude)
                onResult(latLng, "$query, Eswatini")
            }
        }
    } catch (e: Exception) {
        // Search failed - silently ignore
    }
}