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

data class PdfIdCardResult(
    val outputFile: File,
    val pageCount: Int,
    val outputSizeBytes: Long
)

class PdfIdCardTool(
    private val context: Context
) {
    suspend fun createFrontBackSheet(
        frontImageUri: Uri,
        backImageUri: Uri,
        outputName: String,
        pageSize: PdfPageSize = PdfPageSize.A4
    ): PdfIdCardResult = withContext(Dispatchers.IO) {
        val front = decodeBitmapConstrained(context, frontImageUri, maxLongEdge = 1800)
            ?: error("Unable to decode front image.")
        val back = decodeBitmapConstrained(context, backImageUri, maxLongEdge = 1800)
            ?: error("Unable to decode back image.")

        try {
            val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                context = context,
                bucket = DocForgeOutputBucket.DOCUMENTS
            )
            val sanitized = outputName.ifBlank { "id_card_sheet_${System.currentTimeMillis()}" }
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val output = resolveNonConflictingFile(outputDir, sanitized, "pdf")

            val dimensions = when (pageSize) {
                PdfPageSize.LETTER -> 612 to 792
                PdfPageSize.LEGAL -> 612 to 1008
                PdfPageSize.AUTO -> 595 to 842
                PdfPageSize.A4 -> 595 to 842
            }

            val pdf = android.graphics.pdf.PdfDocument()
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

            FileOutputStream(output).use { stream ->
                pdf.writeTo(stream)
            }
            pdf.close()

            PdfIdCardResult(
                outputFile = output,
                pageCount = 1,
                outputSizeBytes = output.length()
            )
        } finally {
            front.recycle()
            back.recycle()
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
