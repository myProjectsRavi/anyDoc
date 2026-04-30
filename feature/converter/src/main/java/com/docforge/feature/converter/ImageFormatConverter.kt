package com.docforge.feature.converter

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.decodeBitmapConstrained
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ImageOutputFormat {
    JPG,
    PNG,
    WEBP
}

data class ImageFormatConversionResult(
    val outputFiles: List<File>,
    val outputSizeBytes: Long
)

class ImageFormatConverter(
    private val context: Context
) {

    suspend fun convertBatch(
        inputUris: List<Uri>,
        outputBaseName: String,
        outputFormat: ImageOutputFormat,
        quality: Int,
        scaleFactor: Float
    ): ImageFormatConversionResult = withContext(Dispatchers.IO) {
        require(inputUris.isNotEmpty()) { "No input images selected." }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.PICTURES
        )

        val base = outputBaseName.ifBlank { "img_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val files = mutableListOf<File>()
        var totalBytes = 0L

        inputUris.forEachIndexed { index, uri ->
            val bitmap = decodeBitmap(uri) ?: error("Failed to decode input image: $uri")

            val scaled = scaleBitmap(bitmap, scaleFactor.coerceIn(0.2f, 3f))
            if (scaled !== bitmap) {
                bitmap.recycle()
            }

            val ext = when (outputFormat) {
                ImageOutputFormat.JPG -> "jpg"
                ImageOutputFormat.PNG -> "png"
                ImageOutputFormat.WEBP -> "webp"
            }
            val output = File(outputDir, "${base}_${index + 1}.$ext")

            FileOutputStream(output).use { stream ->
                val ok = scaled.compress(outputFormat.toCompressFormat(), quality.coerceIn(10, 100), stream)
                require(ok) { "Failed to encode output image for $uri" }
            }
            scaled.recycle()

            files += output
            totalBytes += output.length()
        }

        ImageFormatConversionResult(
            outputFiles = files,
            outputSizeBytes = totalBytes
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return decodeBitmapConstrained(context, uri, maxLongEdge = 2200)
    }

    private fun scaleBitmap(source: Bitmap, scaleFactor: Float): Bitmap {
        if (scaleFactor == 1f) return source
        val width = (source.width * scaleFactor).toInt().coerceAtLeast(1)
        val height = (source.height * scaleFactor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}

private fun ImageOutputFormat.toCompressFormat(): Bitmap.CompressFormat {
    return when (this) {
        ImageOutputFormat.JPG -> Bitmap.CompressFormat.JPEG
        ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageOutputFormat.WEBP -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
    }
}
