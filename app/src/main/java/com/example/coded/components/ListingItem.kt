// File: ui/components/ListingItem.kt
package com.example.coded.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.coded.data.Listing

@Composable
fun ListingItem(
    listing: Listing,
    onContactClick: () -> Unit = {},
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
            // Premium badge
            if (listing.isPremium) {
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

            // Image carousel
            if (listing.imageUrls.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listing.imageUrls.size) { index ->
                        AsyncImage(
                            model = listing.imageUrls[index],
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

            if (listing.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listing.description,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Contact button - only show for free listings
            if (!listing.isPremium && listing.canContactSeller()) {
                Button(
                    onClick = onContactClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Contact Seller")
                }
            } else if (!listing.isPremium) {
                Text(
                    text = "Contact info not available",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Price information
            if (listing.price > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${listing.price}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}