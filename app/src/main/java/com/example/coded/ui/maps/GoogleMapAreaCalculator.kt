package com.example.coded.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlin.math.roundToInt

// Eswatini coordinates for Mbabane, Matsapha, and Manzini areas
object EswatiniCoordinates {
    // Mbabane area (capital city)
    val MBABANE_CENTER = LatLng(-26.305448, 31.136672)

    // Matsapha area (industrial zone)
    val MATSAPHA_CENTER = LatLng(-26.513056, 31.308889)

    // Manzini area (commercial hub)
    val MANZINI_CENTER = LatLng(-26.498837, 31.380817)

    // Default center point between all three areas
    val DEFAULT_CENTER = LatLng(-26.439113, 31.275123) // Midpoint between all three

    // Service area bounds covering Mbabane, Matsapha, and Manzini
    val SERVICE_AREA_BOUNDS = LatLngBounds(
        LatLng(-26.550000, 31.100000), // Southwest (Mbabane-Matsapha area)
        LatLng(-26.300000, 31.450000)  // Northeast (Manzini area)
    )

    // Common service area polygon points for urban areas
    val URBAN_SERVICE_AREA = listOf(
        LatLng(-26.320000, 31.120000), // North Mbabane
        LatLng(-26.320000, 31.150000), // East Mbabane
        LatLng(-26.335000, 31.150000), // Southeast Mbabane
        LatLng(-26.335000, 31.120000)  // Southwest Mbabane
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapAreaCalculator(
    location: LatLng = EswatiniCoordinates.DEFAULT_CENTER,
    onAreaCalculated: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var polygonPoints by remember { mutableStateOf(mutableListOf<LatLng>()) }
    var calculatedArea by remember { mutableStateOf(0.0) }
    var isSatelliteView by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 13f) // Zoom out to see Eswatini areas
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
                Text("Measure Area - Eswatini", fontWeight = FontWeight.Bold)
                Row {
                    // View Toggle - using TextButton instead of IconButton
                    TextButton(
                        onClick = { isSatelliteView = !isSatelliteView },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text(if (isSatelliteView) "Map" else "Satellite")
                    }
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Eswatini Area Quick Select
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3E5F5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Eswatini Service Areas:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mbabane area button
                            FilterChip(
                                selected = false,
                                onClick = {
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                        EswatiniCoordinates.MBABANE_CENTER,
                                        15f
                                    )
                                },
                                label = { Text("Mbabane") },
                                modifier = Modifier.height(32.dp)
                            )

                            // Matsapha area button
                            FilterChip(
                                selected = false,
                                onClick = {
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                        EswatiniCoordinates.MATSAPHA_CENTER,
                                        15f
                                    )
                                },
                                label = { Text("Matsapha") },
                                modifier = Modifier.height(32.dp)
                            )

                            // Manzini area button
                            FilterChip(
                                selected = false,
                                onClick = {
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                        EswatiniCoordinates.MANZINI_CENTER,
                                        15f
                                    )
                                },
                                label = { Text("Manzini") },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                // Instructions
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Using a colored box instead of Icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF1976D2), MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Tap on the map to draw polygon corners",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Connect at least 3 points to calculate area",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
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
                            mapType = if (isSatelliteView) MapType.SATELLITE else MapType.NORMAL,
                            latLngBoundsForCameraTarget = EswatiniCoordinates.SERVICE_AREA_BOUNDS
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            compassEnabled = true
                        ),
                        onMapClick = { latLng ->
                            polygonPoints.add(latLng)
                            if (polygonPoints.size >= 3) {
                                calculatedArea = calculatePolygonArea(polygonPoints)
                            }
                        }
                    ) {
                        // Draw polygon if we have enough points
                        if (polygonPoints.size >= 2) {
                            val polylinePoints = polygonPoints.toMutableList()
                            if (polygonPoints.size >= 3) {
                                // Close the polygon
                                polylinePoints.add(polygonPoints.first())
                            }

                            Polyline(
                                points = polylinePoints,
                                color = Color(0xFF2E7D32),
                                width = 4f
                            )

                            // Fill polygon if closed
                            if (polygonPoints.size >= 3) {
                                Polygon(
                                    points = polygonPoints,
                                    fillColor = Color(0x442E7D32),
                                    strokeColor = Color(0xFF2E7D32),
                                    strokeWidth = 3f
                                )
                            }
                        }

                        // Draw markers for each point
                        polygonPoints.forEachIndexed { index, latLng ->
                            Marker(
                                state = MarkerState(position = latLng),
                                title = "Point ${index + 1}",
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    if (index == 0 && polygonPoints.size >= 3)
                                        BitmapDescriptorFactory.HUE_RED
                                    else BitmapDescriptorFactory.HUE_ORANGE
                                )
                            )
                        }

                        // Mark Eswatini major areas
                        Marker(
                            state = MarkerState(position = EswatiniCoordinates.MBABANE_CENTER),
                            title = "Mbabane",
                            snippet = "Capital City",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        )
                        Marker(
                            state = MarkerState(position = EswatiniCoordinates.MATSAPHA_CENTER),
                            title = "Matsapha",
                            snippet = "Industrial Area",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        )
                        Marker(
                            state = MarkerState(position = EswatiniCoordinates.MANZINI_CENTER),
                            title = "Manzini",
                            snippet = "Commercial Hub",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                        )
                    }

                    // Center crosshair using Box instead of Icon
                    Box(
                        modifier = Modifier.size(32.dp)
                    ) {
                        // Horizontal line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xFF2E7D32))
                                .align(Alignment.Center)
                        )
                        // Vertical line
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .background(Color(0xFF2E7D32))
                                .align(Alignment.Center)
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            polygonPoints.clear()
                            calculatedArea = 0.0
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }

                    OutlinedButton(
                        onClick = {
                            if (polygonPoints.isNotEmpty()) {
                                // Fixed: Using removeAt instead of removeLast for API compatibility
                                polygonPoints.removeAt(polygonPoints.lastIndex)
                                if (polygonPoints.size >= 3) {
                                    calculatedArea = calculatePolygonArea(polygonPoints)
                                } else {
                                    calculatedArea = 0.0
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = polygonPoints.isNotEmpty()
                    ) {
                        Text("Undo")
                    }
                }

                // Area Display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Calculated Area",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                calculatedArea.roundToInt().toString(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "m²",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        if (polygonPoints.size < 3) {
                            Text(
                                "Add ${3 - polygonPoints.size} more points to calculate area",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        } else {
                            Text(
                                "${polygonPoints.size} points | ${(calculatedArea * 10.7639).roundToInt()} ft² | ${"%.2f".format(calculatedArea / 10000.0)} hectares",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Text(
                            "📍 Eswatini Service Area",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAreaCalculated(calculatedArea)
                    onDismiss()
                },
                enabled = calculatedArea > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use This Area (${calculatedArea.roundToInt()} m²)")
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

/**
 * Calculate polygon area using spherical geometry
 * Returns area in square meters
 */
private fun calculatePolygonArea(points: List<LatLng>): Double {
    if (points.size < 3) return 0.0

    val earthRadius = 6371000.0 // Earth's radius in meters
    var area = 0.0

    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]

        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val lon1 = Math.toRadians(p1.longitude)
        val lon2 = Math.toRadians(p2.longitude)

        area += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2))
    }

    area = Math.abs(area * earthRadius * earthRadius / 2.0)
    return area
}