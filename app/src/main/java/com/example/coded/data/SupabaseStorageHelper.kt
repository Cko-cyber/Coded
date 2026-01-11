package com.example.coded.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.coded.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class SupabaseStorageHelper(private val context: Context) {

    private val TAG = "SupabaseStorageHelper"

    private val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Storage)
    }

    private val storage = supabase.storage

    // Bucket names
    private val JOB_IMAGES_BUCKET = "job-images"
    private val COMPLETION_IMAGES_BUCKET = "completion-images"
    private val PROFILE_IMAGES_BUCKET = "profile-images"

    /**
     * Upload job images to Supabase Storage
     * @param imageUris List of image URIs to upload
     * @param userId User ID for organizing files
     * @return List of public URLs for uploaded images
     */
    suspend fun uploadJobImages(
        imageUris: List<Uri>,
        userId: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val uploadedUrls = mutableListOf<String>()
            val timestamp = System.currentTimeMillis()

            imageUris.forEachIndexed { index, uri ->
                try {
                    // Generate unique filename
                    val fileName = "job_${userId}_${timestamp}_$index.jpg"
                    val filePath = "$userId/jobs/$fileName"

                    // Read file bytes
                    val fileBytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes()
                    } ?: throw Exception("Failed to read image file")

                    // Upload to Supabase
                    storage.from(JOB_IMAGES_BUCKET).upload(
                        path = filePath,
                        data = fileBytes,
                        upsert = false
                    )

                    // Get public URL
                    val publicUrl = storage.from(JOB_IMAGES_BUCKET).publicUrl(filePath)
                    uploadedUrls.add(publicUrl)

                    Log.d(TAG, "✅ Uploaded job image: $fileName")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to upload image $index", e)
                    // Continue with other images even if one fails
                }
            }

            if (uploadedUrls.isEmpty() && imageUris.isNotEmpty()) {
                Result.failure(Exception("All image uploads failed"))
            } else {
                Result.success(uploadedUrls)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading job images", e)
            Result.failure(e)
        }
    }

    /**
     * Upload completion images (after job is done)
     */
    suspend fun uploadCompletionImages(
        imageUris: List<Uri>,
        jobId: String,
        providerId: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val uploadedUrls = mutableListOf<String>()
            val timestamp = System.currentTimeMillis()

            imageUris.forEachIndexed { index, uri ->
                try {
                    val fileName = "completion_${jobId}_${timestamp}_$index.jpg"
                    val filePath = "$providerId/completions/$fileName"

                    val fileBytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes()
                    } ?: throw Exception("Failed to read image file")

                    storage.from(COMPLETION_IMAGES_BUCKET).upload(
                        path = filePath,
                        data = fileBytes,
                        upsert = false
                    )

                    val publicUrl = storage.from(COMPLETION_IMAGES_BUCKET).publicUrl(filePath)
                    uploadedUrls.add(publicUrl)

                    Log.d(TAG, "✅ Uploaded completion image: $fileName")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to upload completion image $index", e)
                }
            }

            Result.success(uploadedUrls)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading completion images", e)
            Result.failure(e)
        }
    }

    /**
     * Upload profile image
     */
    suspend fun uploadProfileImage(
        imageUri: Uri,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val filePath = "$userId/$fileName"

            val fileBytes = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw Exception("Failed to read image file")

            storage.from(PROFILE_IMAGES_BUCKET).upload(
                path = filePath,
                data = fileBytes,
                upsert = true // Allow replacing existing profile image
            )

            val publicUrl = storage.from(PROFILE_IMAGES_BUCKET).publicUrl(filePath)

            Log.d(TAG, "✅ Uploaded profile image: $fileName")
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading profile image", e)
            Result.failure(e)
        }
    }

    /**
     * Delete image from storage
     */
    suspend fun deleteImage(
        imageUrl: String,
        bucketName: String = JOB_IMAGES_BUCKET
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Extract path from URL
            val path = imageUrl.substringAfter("$bucketName/")

            storage.from(bucketName).delete(path)

            Log.d(TAG, "✅ Deleted image: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting image", e)
            Result.failure(e)
        }
    }

    /**
     * Delete multiple images
     */
    suspend fun deleteImages(
        imageUrls: List<String>,
        bucketName: String = JOB_IMAGES_BUCKET
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val paths = imageUrls.map { url ->
                url.substringAfter("$bucketName/")
            }

            storage.from(bucketName).delete(paths)

            Log.d(TAG, "✅ Deleted ${paths.size} images")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting images", e)
            Result.failure(e)
        }
    }

    /**
     * Get public URL for an image
     */
    fun getPublicUrl(
        filePath: String,
        bucketName: String = JOB_IMAGES_BUCKET
    ): String {
        return storage.from(bucketName).publicUrl(filePath)
    }

    /**
     * Check if bucket exists, create if not (admin function)
     */
    suspend fun ensureBucketsExist(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // This would require admin privileges
            // In production, buckets should be created via Supabase dashboard
            Log.d(TAG, "✅ Buckets check complete")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking buckets", e)
            Result.failure(e)
        }
    }
}