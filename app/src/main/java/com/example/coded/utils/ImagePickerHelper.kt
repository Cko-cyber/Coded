package com.example.coded.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberImagePicker(
    maxImages: Int = 6,
    onImagesSelected: (List<Uri>) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val selectedUris = uris.take(maxImages)
            onImagesSelected(selectedUris)
        }
    }

    return remember {
        {
            launcher.launch("image/*")
        }
    }
}

// For uploading images to Firebase Storage
suspend fun uploadImageToFirebase(context: Context, uri: Uri, listingId: String, index: Int): String? {
    return try {
        // TODO: Implement Firebase Storage upload
        // For now, return the local URI as string (won't work in production)
        // You'll need to upload to Firebase Storage and return the download URL

        println("⚠️ Image upload not implemented yet. Using local URI.")
        println("   URI: $uri")
        println("   This won't work across devices. Implement Firebase Storage upload.")

        // Placeholder - return empty string for now
        // In production, upload to Firebase Storage and return download URL
        uri.toString()
    } catch (e: Exception) {
        println("❌ Error uploading image: ${e.message}")
        null
    }
}