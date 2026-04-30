package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

enum class PdfPageSize {
    A4,
    LETTER,
    LEGAL,
    AUTO
}

data class PdfCreationOptions(
    val pageSize: PdfPageSize = PdfPageSize.A4
)

data class PdfCreationResult(
    val outputFile: File,
    val pageCount: Int,
    val outputSizeBytes: Long
)

class PdfCreator(
    private val context: Context
) {

    suspend fun createPdfFromImages(
        imageUris: List<Uri>,
        outputName: String,
        options: PdfCreationOptions
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(imageUris.isNotEmpty()) { "No input images selected." }
        val checkCancelled = { coroutineContext.ensureActive() }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "docforge_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val output = File(outputDir, "$sanitized.pdf")

        val pdf = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        imageUris.forEachIndexed { index, uri ->
            checkCancelled()
            val bitmap = decodeBitmap(uri)
                ?: error("Failed to decode image: $uri")

            val pageSize = resolvePageSize(options.pageSize, bitmap)
            val pageInfo = PdfDocument.PageInfo.Builder(pageSize.first, pageSize.second, index + 1).create()
            val page = pdf.startPage(pageInfo)
            drawBitmapFitCenter(page.canvas, bitmap, pageSize.first, pageSize.second, paint)
            pdf.finishPage(page)
            bitmap.recycle()
        }

        FileOutputStream(output).use { stream ->
            pdf.writeTo(stream)
        }
        pdf.close()

        PdfCreationResult(
            outputFile = output,
            pageCount = imageUris.size,
            outputSizeBytes = output.length()
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        // A4 at ~150 DPI is ~1754 px on the long edge; this keeps memory bounded for batch imports.
        return decodeBitmapConstrained(context, uri, maxLongEdge = 1800)
    }

    private fun drawBitmapFitCenter(
        canvas: Canvas,
        bitmap: Bitmap,
        pageWidth: Int,
        pageHeight: Int,
        paint: Paint
    ) {
        canvas.drawColor(Color.WHITE)

        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        val scale = minOf(pageWidth / srcWidth, pageHeight / srcHeight)
        val targetWidth = (srcWidth * scale).roundToInt()
        val targetHeight = (srcHeight * scale).roundToInt()
        val left = (pageWidth - targetWidth) / 2
        val top = (pageHeight - targetHeight) / 2
        val dst = Rect(left, top, left + targetWidth, top + targetHeight)
        canvas.drawBitmap(bitmap, null, dst, paint)
    }

    private fun resolvePageSize(size: PdfPageSize, firstBitmap: Bitmap): Pair<Int, Int> {
        return when (size) {
            PdfPageSize.A4 -> 595 to 842
            PdfPageSize.LETTER -> 612 to 792
            PdfPageSize.LEGAL -> 612 to 1008
            PdfPageSize.AUTO -> firstBitmap.width to firstBitmap.height
        }
    }
}
