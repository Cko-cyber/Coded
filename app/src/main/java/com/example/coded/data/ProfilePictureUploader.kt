package com.example.coded.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ProfilePictureUploader {

    private const val SUPABASE_PROJECT_URL = "https://vxetgoaowehxxifdbdmm.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4ZXRnb2Fvd2VoeHhpZmRiZG1tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MjE4MzYsImV4cCI6MjA3NjQ5NzgzNn0.n7V7iNzNkMjzb2aNq_Z2ANoC7EhmdQcFB4H3tRBnNVM"
    private const val BUCKET_NAME = "profile-pictures"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Upload profile picture using Supabase Edge Function
     */
    suspend fun uploadProfilePicture(
        context: Context,
        uri: Uri,
        userId: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("ProfileUpload", "📤 Starting profile picture upload for user: $userId")

            // Read image bytes
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                Log.e("ProfileUpload", "❌ Failed to read bytes from URI")
                return@withContext Result.failure(Exception("Failed to read image"))
            }

            val fileExtension = uri.fileExtension(context)
            val fileName = "profile_${userId}_${System.currentTimeMillis()}.$fileExtension"

            Log.d("ProfileUpload", "📁 File: $fileName (${bytes.size} bytes)")

            // Step 1: Get signed upload URL from Edge Function
            val urlResponse = getSignedUrlForUpload(fileName, fileExtension)
            if (urlResponse.signedUrl.isEmpty()) {
                Log.e("ProfileUpload", "❌ Failed to get signed URL")
                return@withContext Result.failure(Exception("Failed to get upload URL"))
            }

            Log.d("ProfileUpload", "✅ Got signed URL")

            // Step 2: Upload using signed URL
            val requestBody = ProgressRequestBody(
                data = bytes,
                contentType = "image/$fileExtension"
            ) { progress ->
                onProgress(progress)
            }

            val uploadRequest = Request.Builder()
                .url(urlResponse.signedUrl)
                .put(requestBody)
                .addHeader("Content-Type", "image/$fileExtension")
                .build()

            val uploadResponse = client.newCall(uploadRequest).execute()

            if (!uploadResponse.isSuccessful) {
                val errorBody = uploadResponse.body?.string()
                Log.e("ProfileUpload", "❌ Upload failed: ${uploadResponse.code} - $errorBody")
                return@withContext Result.failure(Exception("Upload failed: ${uploadResponse.code}"))
            }

            Log.d("ProfileUpload", "✅ Upload successful!")
            Log.d("ProfileUpload", "🔗 Public URL: ${urlResponse.publicUrl}")

            Result.success(urlResponse.publicUrl)

        } catch (e: Exception) {
            Log.e("ProfileUpload", "❌ Error uploading profile picture: ${e.message}", e)
            Result.failure(e)
        }
    }

    private data class SignedUrlResponse(
        val signedUrl: String,
        val publicUrl: String
    )

    private suspend fun getSignedUrlForUpload(
        fileName: String,
        fileExtension: String
    ): SignedUrlResponse = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("fileName", fileName)
                put("fileType", "image/$fileExtension")
                put("bucket", BUCKET_NAME)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val edgeFunctionUrl = "$SUPABASE_PROJECT_URL/functions/v1/upload-image"
            Log.d("ProfileUpload", "📡 Calling Edge Function: $edgeFunctionUrl")

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
                Log.e("ProfileUpload", "❌ Edge Function error: ${response.code}")
                Log.e("ProfileUpload", "Response: $responseBody")
                return@withContext SignedUrlResponse("", "")
            }

            val jsonResponse = JSONObject(responseBody)
            val signedUrl = jsonResponse.optString("signedUrl", "")
            val publicUrl = jsonResponse.optString("publicUrl", "")

            if (signedUrl.isEmpty() || publicUrl.isEmpty()) {
                Log.e("ProfileUpload", "❌ Invalid response from Edge Function: $responseBody")
                return@withContext SignedUrlResponse("", "")
            }

            SignedUrlResponse(signedUrl, publicUrl)

        } catch (e: Exception) {
            Log.e("ProfileUpload", "❌ Error calling Edge Function: ${e.message}", e)
            SignedUrlResponse("", "")
        }
    }

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
}

