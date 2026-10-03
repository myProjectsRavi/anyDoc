package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val DEFAULT_MAX_LONG_EDGE = 2200
private const val THUMBNAIL_MAX_LONG_EDGE = 400

/**
 * Decodes a lightweight thumbnail (~400px) suitable for UI preview lists.
 * Uses ~640 KB per image vs ~19 MB for full-resolution decode.
 * Always prefer this for LazyColumn / grid previews to prevent OOM on 4 GB devices.
 */
fun decodeBitmapThumbnail(
    context: Context,
    uri: Uri,
    maxLongEdge: Int = THUMBNAIL_MAX_LONG_EDGE
): Bitmap? = decodeBitmapConstrained(context, uri, maxLongEdge.coerceIn(100, 800))

/**
 * Decodes a bitmap from [uri], down-sampling to fit within [maxLongEdge] pixels,
 * and auto-rotates according to EXIF orientation (gallery imports, camera saves).
 */
fun decodeBitmapConstrained(
    context: Context,
    uri: Uri,
    maxLongEdge: Int = DEFAULT_MAX_LONG_EDGE
): Bitmap? {
    val safeEdge = maxLongEdge.coerceAtLeast(1)
    val bitmapBudgetBytes = bitmapDecodeBudgetBytes(Runtime.getRuntime().maxMemory())

    // ImageDecoder (API 28+) handles EXIF automatically
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val srcWidth = info.size.width.coerceAtLeast(1)
            val srcHeight = info.size.height.coerceAtLeast(1)
            val target = boundedBitmapDecodeTargetSize(
                sourceWidth = srcWidth,
                sourceHeight = srcHeight,
                requestedLongEdge = safeEdge,
                maxBitmapBytes = bitmapBudgetBytes
            )
            if (target.width != srcWidth || target.height != srcHeight) {
                decoder.setTargetSize(target.width, target.height)
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    } else {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        val srcWidth = bounds.outWidth
        val srcHeight = bounds.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            return null
        }

        val target = boundedBitmapDecodeTargetSize(
            sourceWidth = srcWidth,
            sourceHeight = srcHeight,
            requestedLongEdge = safeEdge,
            maxBitmapBytes = bitmapBudgetBytes
        )
        val effectiveLongEdge = max(target.width, target.height)
        val sampleSize = computeSampleSize(srcWidth, srcHeight, effectiveLongEdge)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }?.let { decoded -> applyExifRotation(context, uri, decoded) }
    }
}

/**
 * Reads EXIF orientation from the URI and rotates the bitmap if needed.
 * Used only on the BitmapFactory (pre-P) path; ImageDecoder handles it natively.
 */
private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap? {
    val rotation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                ExifInterface.ORIENTATION_TRANSVERSE -> -270f  // marker for rotate+flip
                ExifInterface.ORIENTATION_TRANSPOSE -> -90f    // marker for rotate+flip
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)

    if (rotation == 0f) return bitmap
    val needsFlip = rotation < 0f
    val actualRotation = if (needsFlip) -rotation else rotation
    val matrix = Matrix().apply {
        postRotate(actualRotation)
        if (needsFlip) postScale(-1f, 1f)
    }
    return try {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    } catch (oom: OutOfMemoryError) {
        // Recycle the source bitmap to free memory, then return null to signal failure
        bitmap.recycle()
        null
    } catch (_: Throwable) {
        bitmap
    }
}

private fun computeSampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
    var sampleSize = 1
    val longest = max(width, height).coerceAtLeast(1)
    while (longest / sampleSize > maxLongEdge) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}


internal data class BitmapDecodeTargetSize(
    val width: Int,
    val height: Int
)

private const val BITMAP_DECODE_BYTES_PER_PIXEL = 4L
private const val BITMAP_DECODE_MIN_BUDGET_BYTES = 4L * 1024L * 1024L
private const val BITMAP_DECODE_MAX_BUDGET_BYTES = 16L * 1024L * 1024L
private const val BITMAP_DECODE_HEAP_DIVISOR = 12L

internal fun bitmapDecodeBudgetBytes(maxHeapBytes: Long): Long {
    val heapAwareBudget = (maxHeapBytes / BITMAP_DECODE_HEAP_DIVISOR).coerceAtLeast(1L)
    return heapAwareBudget.coerceIn(
        BITMAP_DECODE_MIN_BUDGET_BYTES,
        BITMAP_DECODE_MAX_BUDGET_BYTES
    )
}

internal fun boundedBitmapDecodeTargetSize(
    sourceWidth: Int,
    sourceHeight: Int,
    requestedLongEdge: Int,
    maxBitmapBytes: Long
): BitmapDecodeTargetSize {
    require(sourceWidth > 0 && sourceHeight > 0) { "Image dimensions must be positive." }
    require(requestedLongEdge > 0) { "Requested long edge must be positive." }
    require(maxBitmapBytes >= BITMAP_DECODE_BYTES_PER_PIXEL) { "Bitmap budget is too small." }

    val largestSide = max(sourceWidth, sourceHeight).toDouble().coerceAtLeast(1.0)
    val requestedScale = (requestedLongEdge.toDouble() / largestSide).coerceAtMost(1.0)
    val desiredWidth = (sourceWidth.toDouble() * requestedScale)
        .roundToInt()
        .coerceAtLeast(1)
    val desiredHeight = (sourceHeight.toDouble() * requestedScale)
        .roundToInt()
        .coerceAtLeast(1)

    val maxPixels = (maxBitmapBytes / BITMAP_DECODE_BYTES_PER_PIXEL).coerceAtLeast(1L)
    val desiredPixels = desiredWidth.toDouble() * desiredHeight.toDouble()
    if (desiredPixels <= maxPixels.toDouble()) {
        return BitmapDecodeTargetSize(desiredWidth, desiredHeight)
    }

    val budgetScale = sqrt(maxPixels.toDouble() / desiredPixels)
    var width = (desiredWidth.toDouble() * budgetScale).toInt().coerceAtLeast(1)
    var height = (desiredHeight.toDouble() * budgetScale).toInt().coerceAtLeast(1)

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

    return BitmapDecodeTargetSize(width.coerceAtLeast(1), height.coerceAtLeast(1))
}
