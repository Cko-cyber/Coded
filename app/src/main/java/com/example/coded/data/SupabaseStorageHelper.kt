package com.example.coded.data

import ProgressRequestBody
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseStorageHelper {

    // ⚠️ CONFIGURE THESE WITH YOUR ACTUAL SUPABASE CREDENTIALS
    // Example: "https://abcdefgh.supabase.co" (NO trailing slash)
    private const val SUPABASE_PROJECT_URL = "https://vxetgoaowehxxifdbdmm.supabase.co"

    // Your anon/public key from Supabase Dashboard → Settings → API
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4ZXRnb2Fvd2VoeHhpZmRiZG1tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MjE4MzYsImV4cCI6MjA3NjQ5NzgzNn0.n7V7iNzNkMjzb2aNq_Z2ANoC7EhmdQcFB4H3tRBnNVM"

    // Your Edge Function name (default is the folder name where index.ts is)
    private const val EDGE_FUNCTION_NAME = "upload-image"

    // Your bucket name (must match the one in index.ts)
    private const val BUCKET_NAME = "listing-images"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Upload multiple images with progress tracking using Edge Functions
     */
    suspend fun uploadImagesWithProgress(
        context: Context,
        imageUris: List<Uri>,
        listingId: String,
        onProgress: (overallProgress: Float) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {

        // Check configuration first
        if (!isConfigured()) {
            Log.e("SupabaseUpload", "❌ Supabase not configured! Check your credentials.")
            return@withContext emptyList()
        }

        val uploadedUrls = mutableListOf<String>()

        imageUris.forEachIndexed { index, uri ->
            try {
                Log.d("SupabaseUpload", "📤 Starting upload for image ${index + 1}/${imageUris.size}")

                // Read image bytes
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    Log.e("SupabaseUpload", "❌ Failed to read bytes from URI: $uri")
                    return@forEachIndexed
                }

                val fileExtension = uri.fileExtension(context)
                val fileName = "${listingId}_${index}_${System.currentTimeMillis()}.$fileExtension"

                Log.d("SupabaseUpload", "📁 File: $fileName (${bytes.size} bytes)")

                // Step 1: Get signed upload URL from Edge Function
                val urlResponse = getSignedUrlForUpload(fileName, fileExtension)
                if (urlResponse.signedUrl.isEmpty()) {
                    Log.e("SupabaseUpload", "❌ Failed to get signed URL for $fileName")
                    return@forEachIndexed
                }

                Log.d("SupabaseUpload", "✅ Got signed URL")

                // Step 2: Upload using signed URL with progress tracking
                val requestBody = ProgressRequestBody(
                    data = bytes,
                    contentType = "image/$fileExtension"
                ) { progress ->
                    val overallProgress = (index + progress) / imageUris.size
                    onProgress(overallProgress)
                }

                val uploadRequest = Request.Builder()
                    .url(urlResponse.signedUrl)
                    .put(requestBody)
                    .addHeader("Content-Type", "image/$fileExtension")
                    .build()

                val uploadResponse = client.newCall(uploadRequest).execute()

                if (!uploadResponse.isSuccessful) {
                    val errorBody = uploadResponse.body?.string()
                    Log.e("SupabaseUpload", "❌ Upload failed: ${uploadResponse.code} - $errorBody")
                    throw Exception("Upload failed: ${uploadResponse.code}")
                }

                Log.d("SupabaseUpload", "✅ Upload successful!")

                // Step 3: Use the public URL from Edge Function response
                uploadedUrls.add(urlResponse.publicUrl)
                Log.d("SupabaseUpload", "🔗 Public URL: ${urlResponse.publicUrl}")

            } catch (e: Exception) {
                Log.e("SupabaseUpload", "❌ Error uploading image $index: ${e.message}", e)
            }
        }

        Log.d("SupabaseUpload", "✅ Upload complete. ${uploadedUrls.size}/${imageUris.size} successful")
        uploadedUrls
    }

    /**
     * Response from Edge Function
     */
    private data class SignedUrlResponse(
        val signedUrl: String,
        val publicUrl: String
    )

    /**
     * Get signed upload URL from Edge Function
     * Matches your index.ts: expects { fileName, fileType } and returns { signedUrl, publicUrl }
     */
    private suspend fun getSignedUrlForUpload(
        fileName: String,
        fileExtension: String
    ): SignedUrlResponse = withContext(Dispatchers.IO) {
        try {
            // Prepare request matching your Edge Function's expected format
            val jsonBody = JSONObject().apply {
                put("fileName", fileName)
                put("fileType", "image/$fileExtension")
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            // Call your Edge Function
            val edgeFunctionUrl = "$SUPABASE_PROJECT_URL/functions/v1/$EDGE_FUNCTION_NAME"
            Log.d("SupabaseUpload", "📡 Calling Edge Function: $edgeFunctionUrl")

            val request = Request.Builder()
                .url(edgeFunctionUrl)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("SupabaseUpload", "❌ Edge Function error: ${response.code}")
                Log.e("SupabaseUpload", "Response: $responseBody")
                return@withContext SignedUrlResponse("", "")
            }

            // Parse the response from your Edge Function
            val jsonResponse = JSONObject(responseBody)
            val signedUrl = jsonResponse.optString("signedUrl", "")
            val publicUrl = jsonResponse.optString("publicUrl", "")

            if (signedUrl.isEmpty() || publicUrl.isEmpty()) {
                Log.e("SupabaseUpload", "❌ Invalid response from Edge Function: $responseBody")
                return@withContext SignedUrlResponse("", "")
            }

            Log.d("SupabaseUpload", "✅ Received URLs from Edge Function")
            SignedUrlResponse(signedUrl, publicUrl)

        } catch (e: Exception) {
            Log.e("SupabaseUpload", "❌ Error calling Edge Function: ${e.message}", e)
            SignedUrlResponse("", "")
        }
    }

    /**
     * Delete multiple images from Supabase Storage
     */
    suspend fun deleteImagesByUrls(imageUrls: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            imageUrls.forEach { url ->
                val fileName = extractFileNameFromUrl(url)

                // You'll need to create a delete Edge Function or use direct API
                // For now, logging the attempt
                Log.d("SupabaseDelete", "Attempting to delete: $fileName")
                // TODO: Implement delete via Edge Function or direct API
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseDelete", "Error deleting images", e)
            false
        }
    }

    /**
     * Get file extension from URI
     */
    private fun Uri.fileExtension(context: Context): String {
        val mime = context.contentResolver.getType(this)
        return when (mime) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
    }

    /**
     * Extract filename from Supabase public URL
     */
    private fun extractFileNameFromUrl(url: String): String {
        return url.substringAfterLast("/")
    }

    /**
     * Check if Supabase is configured
     */
    fun isConfigured(): Boolean {
        val configured = SUPABASE_PROJECT_URL != "https://your-project-id.supabase.co" &&
                SUPABASE_ANON_KEY != "your-anon-key-here"

        if (!configured) {
            Log.e("SupabaseConfig", "⚠️ ================================")
            Log.e("SupabaseConfig", "⚠️ SUPABASE NOT CONFIGURED!")
            Log.e("SupabaseConfig", "⚠️ ================================")
            Log.e("SupabaseConfig", "Please update SupabaseStorageHelper.kt:")
            Log.e("SupabaseConfig", "1. SUPABASE_PROJECT_URL = your project URL")
            Log.e("SupabaseConfig", "2. SUPABASE_ANON_KEY = your anon key")
            Log.e("SupabaseConfig", "3. EDGE_FUNCTION_NAME = your function folder name")
            Log.e("SupabaseConfig", "")
            Log.e("SupabaseConfig", "Find these in: Supabase Dashboard → Settings → API")
        }

        return configured
    }
}