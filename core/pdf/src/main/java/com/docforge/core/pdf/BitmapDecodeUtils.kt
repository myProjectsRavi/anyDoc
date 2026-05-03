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

private const val DEFAULT_MAX_LONG_EDGE = 2200

/**
 * Decodes a bitmap from [uri], down-sampling to fit within [maxLongEdge] pixels,
 * and auto-rotates according to EXIF orientation (gallery imports, camera saves).
 */
fun decodeBitmapConstrained(
    context: Context,
    uri: Uri,
    maxLongEdge: Int = DEFAULT_MAX_LONG_EDGE
): Bitmap? {
    val safeEdge = maxLongEdge.coerceAtLeast(512)

    // ImageDecoder (API 28+) handles EXIF automatically
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val srcWidth = info.size.width.coerceAtLeast(1)
            val srcHeight = info.size.height.coerceAtLeast(1)
            val longest = max(srcWidth, srcHeight)
            if (longest > safeEdge) {
                val scale = safeEdge.toFloat() / longest.toFloat()
                decoder.setTargetSize(
                    (srcWidth * scale).roundToInt().coerceAtLeast(1),
                    (srcHeight * scale).roundToInt().coerceAtLeast(1)
                )
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

        val sampleSize = computeSampleSize(srcWidth, srcHeight, safeEdge)
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
private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    val rotation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                ExifInterface.ORIENTATION_TRANSVERSE -> 270f
                ExifInterface.ORIENTATION_TRANSPOSE -> 90f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)

    if (rotation == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(rotation) }
    return try {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
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
