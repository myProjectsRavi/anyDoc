package com.docforge.feature.converter

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.StagedOutputRequest
import com.docforge.core.pdf.bitmapDecodeBudgetBytes
import com.docforge.core.pdf.decodeBitmapConstrained
import com.docforge.core.pdf.withStagedOutputFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

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

        val safeScale = scaleFactor.coerceIn(0.2f, 3f)
        val safeQuality = quality.coerceIn(10, 100)
        val ext = when (outputFormat) {
            ImageOutputFormat.JPG -> "jpg"
            ImageOutputFormat.PNG -> "png"
            ImageOutputFormat.WEBP -> "webp"
        }

        val requests = inputUris.mapIndexed { index, uri ->
            StagedOutputRequest(
                baseName = "${base}_${index + 1}",
                extension = ext
            ) { stagedFile ->
                val bitmap = decodeBitmap(uri) ?: error("Failed to decode input image: $uri")
                var scaled: Bitmap? = null
                try {
                    scaled = scaleBitmap(
                        source = bitmap,
                        scaleFactor = safeScale,
                        maxBitmapBytes = bitmapDecodeBudgetBytes(Runtime.getRuntime().maxMemory())
                    )
                    FileOutputStream(stagedFile).use { stream ->
                        val ok = scaled.compress(outputFormat.toCompressFormat(), safeQuality, stream)
                        require(ok) { "Failed to encode output image for $uri" }
                    }
                } finally {
                    if (scaled != null && scaled !== bitmap && !scaled.isRecycled) {
                        scaled.recycle()
                    }
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        }

        val results = withStagedOutputFiles(
            directory = outputDir,
            requests = requests
        )
        val files = results.map { it.outputFile }

        ImageFormatConversionResult(
            outputFiles = files,
            outputSizeBytes = files.sumOf { it.length() }
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return decodeBitmapConstrained(context, uri, maxLongEdge = 2200)
    }

    private fun scaleBitmap(
        source: Bitmap,
        scaleFactor: Float,
        maxBitmapBytes: Long
    ): Bitmap {
        val target = boundedScaledBitmapSize(
            sourceWidth = source.width,
            sourceHeight = source.height,
            scaleFactor = scaleFactor,
            maxBitmapBytes = maxBitmapBytes
        )
        if (target.width == source.width && target.height == source.height) return source
        return Bitmap.createScaledBitmap(source, target.width, target.height, true)
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


internal data class ScaledBitmapSize(
    val width: Int,
    val height: Int
)

private const val IMAGE_SCALE_BYTES_PER_PIXEL = 4L

internal fun boundedScaledBitmapSize(
    sourceWidth: Int,
    sourceHeight: Int,
    scaleFactor: Float,
    maxBitmapBytes: Long
): ScaledBitmapSize {
    require(sourceWidth > 0 && sourceHeight > 0) { "Image dimensions must be positive." }
    require(scaleFactor.isFinite() && scaleFactor > 0f) { "Scale factor must be finite and positive." }
    require(maxBitmapBytes >= IMAGE_SCALE_BYTES_PER_PIXEL) { "Bitmap budget is too small." }

    val desiredWidth = (sourceWidth.toDouble() * scaleFactor.toDouble())
        .coerceIn(1.0, Int.MAX_VALUE.toDouble())
    val desiredHeight = (sourceHeight.toDouble() * scaleFactor.toDouble())
        .coerceIn(1.0, Int.MAX_VALUE.toDouble())

    val maxPixels = (maxBitmapBytes / IMAGE_SCALE_BYTES_PER_PIXEL).coerceAtLeast(1L)
    val desiredPixels = desiredWidth * desiredHeight
    if (desiredPixels <= maxPixels.toDouble()) {
        return ScaledBitmapSize(
            width = desiredWidth.toInt().coerceAtLeast(1),
            height = desiredHeight.toInt().coerceAtLeast(1)
        )
    }

    val budgetScale = sqrt(maxPixels.toDouble() / desiredPixels)
    var width = (desiredWidth * budgetScale).toInt().coerceAtLeast(1)
    var height = (desiredHeight * budgetScale).toInt().coerceAtLeast(1)

    if (width.toLong() * height.toLong() > maxPixels) {
        if (width >= height) {
            width = minOf(width.toLong(), (maxPixels / height.toLong()).coerceAtLeast(1L))
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        } else {
            height = minOf(height.toLong(), (maxPixels / width.toLong()).coerceAtLeast(1L))
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }
    }

    return ScaledBitmapSize(width, height)
}
