package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max

data class PdfCompareResult(
    val outputFile: File,
    val outputSizeBytes: Long,
    val pageCount: Int,
    val diffPercentages: List<Float>
)

/**
 * Compares two PDFs page-by-page by rendering and diffing bitmaps.
 * Outputs a new PDF with diff highlights.
 *
 * Sprint 4 feature — PDF Compare tool.
 */
class PdfCompareTool(
    private val context: Context
) {

    /**
     * Compares [leftUri] and [rightUri] page-by-page.
     * Produces a diff PDF where identical pixels are greyed out and changed pixels are highlighted in red.
     *
     * @param renderScale  scale factor for rendering (1.0 = 72 DPI, 2.0 = 144 DPI).
     * @param diffThreshold per-channel difference threshold (0–255) below which pixels are considered equal.
     */
    suspend fun compare(
        leftUri: Uri,
        rightUri: Uri,
        outputName: String,
        renderScale: Float = 1.5f,
        diffThreshold: Int = 30
    ): PdfCompareResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        val threshold = diffThreshold.coerceIn(0, 255)
        val scale = renderScale.coerceIn(0.5f, 4f)

        context.withUriCopiedToCacheFile(leftUri, prefix = "docforge_cmp_left_", suffix = ".pdf") { leftFile ->
            context.withUriCopiedToCacheFile(rightUri, prefix = "docforge_cmp_right_", suffix = ".pdf") { rightFile ->
                val leftPfd = ParcelFileDescriptor.open(leftFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val rightPfd = ParcelFileDescriptor.open(rightFile, ParcelFileDescriptor.MODE_READ_ONLY)

                leftPfd.use { lPfd ->
                    rightPfd.use { rPfd ->
                        PdfRenderer(lPfd).use { leftRenderer ->
                            PdfRenderer(rPfd).use { rightRenderer ->
                                val pageCount = max(leftRenderer.pageCount, rightRenderer.pageCount)
                                require(pageCount > 0) { "Both PDFs have no pages." }

                                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                                    context = context,
                                    bucket = DocForgeOutputBucket.DOCUMENTS
                                )
                                val sanitized = outputName.ifBlank { "compare_${System.currentTimeMillis()}" }
                                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

                                val pdfDoc = android.graphics.pdf.PdfDocument()
                                val diffPcts = mutableListOf<Float>()

                                for (pageIndex in 0 until pageCount) {
                                    checkCancelled()
                                    val leftBmp = renderPage(leftRenderer, pageIndex, scale)
                                    val rightBmp = renderPage(rightRenderer, pageIndex, scale)

                                    val (diffBmp, pct) = diffBitmaps(leftBmp, rightBmp, threshold)
                                    diffPcts += pct

                                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                        diffBmp.width, diffBmp.height, pageIndex + 1
                                    ).create()
                                    val page = pdfDoc.startPage(pageInfo)
                                    page.canvas.drawBitmap(diffBmp, 0f, 0f, null)
                                    pdfDoc.finishPage(page)

                                    leftBmp?.recycle()
                                    rightBmp?.recycle()
                                    diffBmp.recycle()
                                }

                                FileOutputStream(outputFile).use { pdfDoc.writeTo(it) }
                                pdfDoc.close()

                                PdfCompareResult(
                                    outputFile = outputFile,
                                    outputSizeBytes = outputFile.length(),
                                    pageCount = pageCount,
                                    diffPercentages = diffPcts
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderPage(renderer: PdfRenderer, pageIndex: Int, scale: Float): Bitmap? {
        if (pageIndex >= renderer.pageCount) return null
        val page = renderer.openPage(pageIndex)
        val w = (page.width * scale).toInt().coerceAtLeast(1)
        val h = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    /**
     * Returns a diff bitmap and the percentage of pixels that differ.
     * - Identical pixels → light grey
     * - Changed pixels → red overlay
     * - Missing page → entire page marked as red
     */
    private fun diffBitmaps(left: Bitmap?, right: Bitmap?, threshold: Int): Pair<Bitmap, Float> {
        if (left == null && right == null) {
            val blank = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
            blank.eraseColor(Color.LTGRAY)
            return blank to 0f
        }
        if (left == null) return right!!.copy(Bitmap.Config.ARGB_8888, false) to 100f
        if (right == null) return left.copy(Bitmap.Config.ARGB_8888, false) to 100f

        val w = max(left.width, right.width)
        val h = max(left.height, right.height)
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.eraseColor(Color.WHITE)

        var diffPixels = 0
        val totalPixels = w * h

        for (y in 0 until h) {
            for (x in 0 until w) {
                val lPixel = if (x < left.width && y < left.height) left.getPixel(x, y) else Color.WHITE
                val rPixel = if (x < right.width && y < right.height) right.getPixel(x, y) else Color.WHITE

                val dr = abs(Color.red(lPixel) - Color.red(rPixel))
                val dg = abs(Color.green(lPixel) - Color.green(rPixel))
                val db = abs(Color.blue(lPixel) - Color.blue(rPixel))

                if (dr > threshold || dg > threshold || db > threshold) {
                    output.setPixel(x, y, Color.argb(200, 255, 50, 50))
                    diffPixels++
                } else {
                    // Greyed-out version of original
                    val grey = (Color.red(lPixel) * 0.3f + Color.green(lPixel) * 0.59f + Color.blue(lPixel) * 0.11f).toInt()
                    output.setPixel(x, y, Color.rgb(grey, grey, grey))
                }
            }
        }

        val pct = if (totalPixels > 0) (diffPixels.toFloat() / totalPixels * 100f) else 0f
        return output to pct
    }
}
