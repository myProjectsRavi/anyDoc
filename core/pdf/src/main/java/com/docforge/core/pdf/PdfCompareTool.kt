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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

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
internal data class CompareRasterSize(val width: Int, val height: Int)

internal object CompareRasterBudget {
    private const val BYTES_PER_PIXEL = 4L
    private const val MIN_BUDGET_BYTES = 12L * 1024L * 1024L
    private const val MAX_BUDGET_BYTES = 48L * 1024L * 1024L

    fun heapAwareBudgetBytes(maxHeapBytes: Long): Long =
        (maxHeapBytes / 8L).coerceIn(MIN_BUDGET_BYTES, MAX_BUDGET_BYTES)

    fun fit(
        left: CompareRasterSize?,
        right: CompareRasterSize?,
        budgetBytes: Long
    ): Pair<CompareRasterSize?, CompareRasterSize?> {
        require(budgetBytes >= BYTES_PER_PIXEL) { "Raster budget is too small." }
        val requestedBytes = workingSetBytes(left, right)
        if (requestedBytes <= budgetBytes) return left to right

        val scale = sqrt(budgetBytes.toDouble() / requestedBytes.toDouble())
        fun scaled(size: CompareRasterSize?): CompareRasterSize? = size?.let {
            CompareRasterSize(
                width = floor(it.width * scale).toInt().coerceAtLeast(1),
                height = floor(it.height * scale).toInt().coerceAtLeast(1)
            )
        }

        var fittedLeft = scaled(left)
        var fittedRight = scaled(right)
        if (workingSetBytes(fittedLeft, fittedRight) <= budgetBytes) return fittedLeft to fittedRight

        fun scaledAt(factor: Double, size: CompareRasterSize?): CompareRasterSize? = size?.let {
            CompareRasterSize(
                floor(it.width * factor).toInt().coerceAtLeast(1),
                floor(it.height * factor).toInt().coerceAtLeast(1)
            )
        }
        require(workingSetBytes(scaledAt(0.0, left), scaledAt(0.0, right)) <= budgetBytes)
        var low = 0.0
        var high = scale
        repeat(48) {
            val mid = (low + high) / 2.0
            val nextLeft = scaledAt(mid, left)
            val nextRight = scaledAt(mid, right)
            if (workingSetBytes(nextLeft, nextRight) <= budgetBytes) {
                low = mid
                fittedLeft = nextLeft
                fittedRight = nextRight
            } else {
                high = mid
            }
        }
        return fittedLeft to fittedRight
    }

    fun workingSetBytes(left: CompareRasterSize?, right: CompareRasterSize?): Long {
        fun pixels(size: CompareRasterSize?): Long = size?.let {
            Math.multiplyExact(it.width.toLong(), it.height.toLong())
        } ?: 0L
        val leftPixels = pixels(left)
        val rightPixels = pixels(right)
        val diffPixels = when {
            left == null -> rightPixels
            right == null -> leftPixels
            else -> Math.multiplyExact(max(left.width, right.width).toLong(), max(left.height, right.height).toLong())
        }
        return Math.multiplyExact(Math.addExact(Math.addExact(leftPixels, rightPixels), diffPixels), BYTES_PER_PIXEL)
    }
}

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
                                val stagedResult = withStagedOutputFile(
                                    directory = outputDir,
                                    baseName = sanitized,
                                    extension = "pdf"
                                ) { stagedFile ->
                                    val diffPcts = mutableListOf<Float>()
                                    val pdfDoc = android.graphics.pdf.PdfDocument()
                                    try {
                                        for (pageIndex in 0 until pageCount) {
                                            checkCancelled()
                                            var leftBmp: Bitmap? = null
                                            var rightBmp: Bitmap? = null
                                            var diffBmp: Bitmap? = null
                                            try {
                                                val requestedLeft = requestedSize(leftRenderer, pageIndex, scale)
                                                val requestedRight = requestedSize(rightRenderer, pageIndex, scale)
                                                val (leftSize, rightSize) = CompareRasterBudget.fit(
                                                    left = requestedLeft,
                                                    right = requestedRight,
                                                    budgetBytes = CompareRasterBudget.heapAwareBudgetBytes(Runtime.getRuntime().maxMemory())
                                                )
                                                leftBmp = renderPage(leftRenderer, pageIndex, leftSize)
                                                rightBmp = renderPage(rightRenderer, pageIndex, rightSize)

                                                val diff = diffBitmaps(leftBmp, rightBmp, threshold)
                                                diffBmp = diff.first
                                                diffPcts += diff.second

                                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                                    diffBmp.width,
                                                    diffBmp.height,
                                                    pageIndex + 1
                                                ).create()
                                                val page = pdfDoc.startPage(pageInfo)
                                                page.canvas.drawBitmap(diffBmp, 0f, 0f, null)
                                                pdfDoc.finishPage(page)
                                            } finally {
                                                leftBmp?.recycle()
                                                rightBmp?.recycle()
                                                diffBmp?.recycle()
                                            }
                                        }

                                        FileOutputStream(stagedFile).use { pdfDoc.writeTo(it) }
                                    } finally {
                                        pdfDoc.close()
                                    }
                                    diffPcts.toList()
                                }

                                PdfCompareResult(
                                    outputFile = stagedResult.outputFile,
                                    outputSizeBytes = stagedResult.outputFile.length(),
                                    pageCount = pageCount,
                                    diffPercentages = stagedResult.value
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestedSize(renderer: PdfRenderer, pageIndex: Int, scale: Float): CompareRasterSize? {
        if (pageIndex >= renderer.pageCount) return null
        renderer.openPage(pageIndex).use { page ->
            return CompareRasterSize(
                width = (page.width * scale).toInt().coerceAtLeast(1),
                height = (page.height * scale).toInt().coerceAtLeast(1)
            )
        }
    }

    private fun renderPage(renderer: PdfRenderer, pageIndex: Int, size: CompareRasterSize?): Bitmap? {
        if (size == null || pageIndex >= renderer.pageCount) return null
        renderer.openPage(pageIndex).use { page ->
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            return try {
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (t: Throwable) {
                bitmap.recycle()
                throw t
            }
        }
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

        var diffPixels = 0L
        val totalPixels = Math.multiplyExact(w.toLong(), h.toLong())

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
