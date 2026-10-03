package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PdfIdCardResult(
    val outputFile: File,
    val pageCount: Int,
    val outputSizeBytes: Long
)

internal object IdCardDecodeBudget {
    private const val BYTES_PER_ARGB_PIXEL = 4L
    private const val PEAK_BITMAP_COUNT = 3L
    private const val HEAP_BUDGET_DIVISOR = 8L
    private const val MIN_LONG_EDGE = 512
    private const val MAX_LONG_EDGE = 1800

    /**
     * The front bitmap remains resident while the back image is decoded. On the pre-P
     * EXIF path, the decoded back source and its rotated replacement can briefly coexist,
     * so reserve for three square ARGB_8888 bitmaps at the selected long edge.
     */
    fun maxLongEdge(maxHeapBytes: Long): Int {
        val bitmapBudgetBytes = maxHeapBytes.coerceAtLeast(0L) / HEAP_BUDGET_DIVISOR
        val bytesPerSquareEdge = BYTES_PER_ARGB_PIXEL * PEAK_BITMAP_COUNT
        val edge = sqrt(bitmapBudgetBytes.toDouble() / bytesPerSquareEdge.toDouble()).toInt()
        return edge.coerceIn(MIN_LONG_EDGE, MAX_LONG_EDGE)
    }
}

class PdfIdCardTool(
    private val context: Context
) {
    suspend fun createFrontBackSheet(
        frontImageUri: Uri,
        backImageUri: Uri,
        outputName: String,
        pageSize: PdfPageSize = PdfPageSize.A4
    ): PdfIdCardResult = withContext(Dispatchers.IO) {
        val decodeLongEdge = IdCardDecodeBudget.maxLongEdge(Runtime.getRuntime().maxMemory())
        val front = decodeBitmapConstrained(context, frontImageUri, maxLongEdge = decodeLongEdge)
            ?: error("Unable to decode front image.")
        var back: Bitmap? = null

        try {
            back = decodeBitmapConstrained(context, backImageUri, maxLongEdge = decodeLongEdge)
                ?: error("Unable to decode back image.")

            val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                context = context,
                bucket = DocForgeOutputBucket.DOCUMENTS
            )
            val sanitized = outputName.ifBlank { "id_card_sheet_${System.currentTimeMillis()}" }
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")

            val dimensions = when (pageSize) {
                PdfPageSize.LETTER -> 612 to 792
                PdfPageSize.LEGAL -> 612 to 1008
                PdfPageSize.AUTO -> 595 to 842
                PdfPageSize.A4 -> 595 to 842
            }

            val stagedResult = withStagedOutputFile(
                directory = outputDir,
                baseName = sanitized,
                extension = "pdf"
            ) { stagedFile ->
                val pdf = android.graphics.pdf.PdfDocument()
                try {
                    val page = pdf.startPage(
                        android.graphics.pdf.PdfDocument.PageInfo.Builder(
                            dimensions.first,
                            dimensions.second,
                            1
                        ).create()
                    )

                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val margin = 24
                    val gap = 18
                    val halfHeight = (dimensions.second - (margin * 2) - gap) / 2
                    val usableWidth = dimensions.first - (margin * 2)

                    val topRect = Rect(margin, margin, margin + usableWidth, margin + halfHeight)
                    val bottomTop = margin + halfHeight + gap
                    val bottomRect = Rect(margin, bottomTop, margin + usableWidth, bottomTop + halfHeight)

                    drawBitmapFitCenter(canvas, front, topRect, paint)
                    drawBitmapFitCenter(canvas, back, bottomRect, paint)

                    paint.color = Color.DKGRAY
                    paint.textSize = 12f
                    canvas.drawText("Front", margin.toFloat(), (topRect.bottom + 14).toFloat(), paint)
                    canvas.drawText("Back", margin.toFloat(), (bottomRect.bottom + 14).toFloat(), paint)

                    pdf.finishPage(page)
                    FileOutputStream(stagedFile).use { stream ->
                        pdf.writeTo(stream)
                    }
                } finally {
                    pdf.close()
                }
            }

            PdfIdCardResult(
                outputFile = stagedResult.outputFile,
                pageCount = 1,
                outputSizeBytes = stagedResult.outputFile.length()
            )
        } finally {
            if (!front.isRecycled) {
                front.recycle()
            }
            back?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun drawBitmapFitCenter(
        canvas: Canvas,
        bitmap: Bitmap,
        targetRect: Rect,
        paint: Paint
    ) {
        val srcWidth = bitmap.width.toFloat().coerceAtLeast(1f)
        val srcHeight = bitmap.height.toFloat().coerceAtLeast(1f)
        val dstWidth = targetRect.width().toFloat().coerceAtLeast(1f)
        val dstHeight = targetRect.height().toFloat().coerceAtLeast(1f)

        val scale = minOf(dstWidth / srcWidth, dstHeight / srcHeight)
        val renderWidth = (srcWidth * scale).roundToInt().coerceAtLeast(1)
        val renderHeight = (srcHeight * scale).roundToInt().coerceAtLeast(1)

        val left = targetRect.left + ((targetRect.width() - renderWidth) / 2)
        val top = targetRect.top + ((targetRect.height() - renderHeight) / 2)
        val dst = Rect(left, top, left + renderWidth, top + renderHeight)
        canvas.drawBitmap(bitmap, null, dst, paint)
    }
}
