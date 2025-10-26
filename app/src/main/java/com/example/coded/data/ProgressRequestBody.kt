import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

/**
 * Custom RequestBody that reports progress while uploading.
 */
class ProgressRequestBody(
    private val data: ByteArray,
    private val contentType: String,
    private val onProgress: (progress: Float) -> Unit
) : RequestBody() {

    override fun contentType() = contentType.toMediaTypeOrNull()

    override fun contentLength() = data.size.toLong()

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
