// File: ui/components/ListingItem.kt
package com.example.coded.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.coded.data.Listing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingItem(
    listing: Listing,
    onMessageClick: () -> Unit = {},
    onBookNowClick: () -> Unit = {},
    onItemClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onItemClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Premium badge - show for premium listings
            if (listing.listingTier.equals("PREMIUM", ignoreCase = true)) {
                Badge(
                    modifier = Modifier.align(Alignment.End),
                    containerColor = Color.Yellow,
                    contentColor = Color.Black
                ) {
                    Text(
                        text = "PREMIUM",
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Image carousel - use image_urls from your Listing class
            if (listing.image_urls.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listing.image_urls.size) { index ->
                        AsyncImage(
                            model = listing.image_urls[index],
                            contentDescription = "Listing image ${index + 1}",
                            modifier = Modifier
                                .width(280.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Listing details
            Text(
                text = listing.breed,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (listing.full_details.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listing.full_details,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }

            if (listing.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${listing.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Additional details
            if (listing.age.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Age: ${listing.age}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (listing.vaccination_status.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Vaccination: ${listing.vaccination_status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (listing.deworming.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Deworming: ${listing.deworming}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price information - use your getDisplayPrice method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.getDisplayPrice(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Tier indicator
                if (!listing.listingTier.equals("FREE", ignoreCase = true)) {
                    Text(
                        text = listing.listingTier,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ALWAYS SHOW BOTH BUTTONS - regardless of tier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Message Button
                Button(
                    onClick = onMessageClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF013B33)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Message")
                }

                // Book Now Button
                Button(
                    onClick = onBookNowClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Book Now")
                }
            }
        }
    }
}