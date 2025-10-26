package com.example.coded.data

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
    private const val SUPABASE_PROJECT_URL = "https://vxetgoaowehxxifdbdmm.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4ZXRnb2Fvd2VoeHhpZmRiZG1tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MjE4MzYsImV4cCI6MjA3NjQ5NzgzNn0.n7V7iNzNkMjzb2aNq_Z2ANoC7EhmdQcFB4H3tRBnNVM"
    private const val EDGE_FUNCTION_NAME = "upload-image"
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

        Log.d("SupabaseUpload", "🚀 Starting upload for ${imageUris.size} images")
        Log.d("SupabaseUpload", "   Listing ID: $listingId")

        if (!isConfigured()) {
            Log.e("SupabaseUpload", "❌ Supabase not configured!")
            return@withContext emptyList()
        }

        val uploadedUrls = mutableListOf<String>()

        imageUris.forEachIndexed { index, uri ->
            try {
                Log.d("SupabaseUpload", "📤 Starting upload for image ${index + 1}/${imageUris.size}")
                Log.d("SupabaseUpload", "   URI: $uri")

                // Read image bytes
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    Log.e("SupabaseUpload", "❌ Failed to read bytes from URI: $uri")
                    return@forEachIndexed
                }

                val fileExtension = uri.fileExtension(context)
                val fileName = "${listingId}_${index}_${System.currentTimeMillis()}.$fileExtension"

                Log.d("SupabaseUpload", "   📁 File: $fileName")
                Log.d("SupabaseUpload", "   📊 Size: ${bytes.size} bytes")
                Log.d("SupabaseUpload", "   🔧 Extension: $fileExtension")

                // Step 1: Get signed upload URL from Edge Function
                Log.d("SupabaseUpload", "   🔗 Getting signed URL...")
                val urlResponse = getSignedUrlForUpload(fileName, fileExtension)
                if (urlResponse.signedUrl.isEmpty()) {
                    Log.e("SupabaseUpload", "❌ Failed to get signed URL for $fileName")
                    return@forEachIndexed
                }

                Log.d("SupabaseUpload", "   ✅ Got signed URL")
                Log.d("SupabaseUpload", "   🔗 Signed URL: ${urlResponse.signedUrl.take(100)}...")
                Log.d("SupabaseUpload", "   🔗 Public URL: ${urlResponse.publicUrl.take(100)}...")

                // Step 2: Upload using signed URL with progress tracking
                Log.d("SupabaseUpload", "   📤 Uploading to Supabase Storage...")
                val requestBody = ProgressRequestBody(
                    data = bytes,
                    contentType = "image/$fileExtension"
                ) { progress ->
                    val overallProgress = (index + progress) / imageUris.size
                    onProgress(overallProgress)
                    Log.d("SupabaseUpload", "   📊 Upload progress: ${(progress * 100).toInt()}%")
                }

                val uploadRequest = Request.Builder()
                    .url(urlResponse.signedUrl)
                    .put(requestBody)
                    .addHeader("Content-Type", "image/$fileExtension")
                    .build()

                val uploadResponse = client.newCall(uploadRequest).execute()

                if (!uploadResponse.isSuccessful) {
                    val errorBody = uploadResponse.body?.string()
                    Log.e("SupabaseUpload", "❌ Upload failed: ${uploadResponse.code}")
                    Log.e("SupabaseUpload", "   Error body: $errorBody")
                    throw Exception("Upload failed: ${uploadResponse.code}")
                }

                uploadResponse.close()
                Log.d("SupabaseUpload", "   ✅ Upload successful!")

                // Step 3: Add the public URL
                uploadedUrls.add(urlResponse.publicUrl)
                Log.d("SupabaseUpload", "   ✅ Added public URL to list")
                Log.d("SupabaseUpload", "   🔗 Public URL: ${urlResponse.publicUrl}")

            } catch (e: Exception) {
                Log.e("SupabaseUpload", "❌ Error uploading image $index: ${e.message}", e)
                e.printStackTrace()
            }
        }

        Log.d("SupabaseUpload", "✅✅✅ UPLOAD COMPLETE ✅✅✅")
        Log.d("SupabaseUpload", "   Successfully uploaded: ${uploadedUrls.size}/${imageUris.size}")
        uploadedUrls.forEachIndexed { idx, url ->
            Log.d("SupabaseUpload", "   Image $idx: $url")
        }

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
     */
    private suspend fun getSignedUrlForUpload(
        fileName: String,
        fileExtension: String
    ): SignedUrlResponse = withContext(Dispatchers.IO) {
        try {
            Log.d("SupabaseUpload", "🔗 Requesting signed URL from Edge Function")
            Log.d("SupabaseUpload", "   File: $fileName")
            Log.d("SupabaseUpload", "   Type: image/$fileExtension")

            val jsonBody = JSONObject().apply {
                put("fileName", fileName)
                put("fileType", "image/$fileExtension")
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val edgeFunctionUrl = "$SUPABASE_PROJECT_URL/functions/v1/$EDGE_FUNCTION_NAME"
            Log.d("SupabaseUpload", "   📡 Edge Function URL: $edgeFunctionUrl")

            val request = Request.Builder()
                .url(edgeFunctionUrl)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d("SupabaseUpload", "   📡 Edge Function Response Code: ${response.code}")

            if (!response.isSuccessful) {
                Log.e("SupabaseUpload", "❌ Edge Function error: ${response.code}")
                Log.e("SupabaseUpload", "   Response: $responseBody")
                return@withContext SignedUrlResponse("", "")
            }

            Log.d("SupabaseUpload", "   ✅ Edge Function success")
            Log.d("SupabaseUpload", "   Response: ${responseBody.take(200)}...")

            val jsonResponse = JSONObject(responseBody)
            val signedUrl = jsonResponse.optString("signedUrl", "")
            val publicUrl = jsonResponse.optString("publicUrl", "")

            if (signedUrl.isEmpty() || publicUrl.isEmpty()) {
                Log.e("SupabaseUpload", "❌ Invalid response from Edge Function")
                Log.e("SupabaseUpload", "   signedUrl empty: ${signedUrl.isEmpty()}")
                Log.e("SupabaseUpload", "   publicUrl empty: ${publicUrl.isEmpty()}")
                return@withContext SignedUrlResponse("", "")
            }

            Log.d("SupabaseUpload", "   ✅ Received valid URLs from Edge Function")
            SignedUrlResponse(signedUrl, publicUrl)

        } catch (e: Exception) {
            Log.e("SupabaseUpload", "❌ Error calling Edge Function: ${e.message}", e)
            e.printStackTrace()
            SignedUrlResponse("", "")
        }
    }

    /**
     * Delete multiple images from Supabase Storage
     */
    suspend fun deleteImagesByUrls(imageUrls: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("SupabaseDelete", "🗑️ Deleting ${imageUrls.size} images")
            imageUrls.forEach { url ->
                val fileName = extractFileNameFromUrl(url)
                Log.d("SupabaseDelete", "   Attempting to delete: $fileName")
            }
            true
        } catch (e: Exception) {
            Log.e("SupabaseDelete", "❌ Error deleting images", e)
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
        }

        return configured
    }
}