package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_MAX_LONG_EDGE = 2200

fun decodeBitmapConstrained(
    context: Context,
    uri: Uri,
    maxLongEdge: Int = DEFAULT_MAX_LONG_EDGE
): Bitmap? {
    val safeEdge = maxLongEdge.coerceAtLeast(512)

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
        }
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
