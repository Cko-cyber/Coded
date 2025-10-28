package com.example.coded.data

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink

class ProgressRequestBody(
    private val data: ByteArray,
    private val contentType: String,
    private val onProgress: (progress: Float) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = data.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val total = data.size.toLong()
        var uploaded: Long = 0

        data.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress((uploaded.toFloat() / total))
            }
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}