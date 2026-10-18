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

/** PDF page dimensions are points; render scale affects only raster pixel density. */
internal fun scaledCompareRasterSize(pageSize: CompareRasterSize, scale: Float): CompareRasterSize =
    CompareRasterSize(
        width = (pageSize.width * scale).toInt().coerceAtLeast(1),
        height = (pageSize.height * scale).toInt().coerceAtLeast(1)
    )

/** Preserve the larger source page bounds when compared pages have different dimensions. */
internal fun compareOutputPageSize(
    left: CompareRasterSize?,
    right: CompareRasterSize?
): CompareRasterSize {
    require(left != null || right != null) { "At least one PDF page must exist." }
    return CompareRasterSize(
        width = max(left?.width ?: 0, right?.width ?: 0),
        height = max(left?.height ?: 0, right?.height ?: 0)
    )
}

/** Preserve normal scale clamping while preventing NaN from collapsing page rasters to 1x1. */
internal fun normalizedCompareRenderScale(requested: Float): Float =
    if (requested.isNaN()) 1.5f else requested.coerceIn(0.5f, 4f)

internal object CompareRasterBudget {
    private const val BYTES_PER_PIXEL = 4L
    // A one-pixel left, right, and diff bitmap need 12 bytes. Do not reserve 12 MiB on tiny heaps.
    private const val MIN_BUDGET_BYTES = 3L * BYTES_PER_PIXEL
    private const val MAX_BUDGET_BYTES = 48L * 1024L * 1024L

    fun heapAwareBudgetBytes(maxHeapBytes: Long, occupiedHeapBytes: Long = 0L): Long {
        val remaining = (maxHeapBytes - occupiedHeapBytes.coerceIn(0L, maxHeapBytes.coerceAtLeast(0L)))
            .coerceAtLeast(0L)
        return (remaining / 8L).coerceIn(MIN_BUDGET_BYTES, MAX_BUDGET_BYTES)
    }

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
            require(it.width > 0 && it.height > 0) { "Raster dimensions must be positive." }
            try {
                Math.multiplyExact(it.width.toLong(), it.height.toLong())
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
        } ?: 0L
        val leftPixels = pixels(left)
        val rightPixels = pixels(right)
        val diffPixels = when {
            left == null -> rightPixels
            right == null -> leftPixels
            else -> Math.multiplyExact(max(left.width, right.width).toLong(), max(left.height, right.height).toLong())
        }
        val totalPixels = listOf(leftPixels, rightPixels, diffPixels).fold(0L) { total, pixels ->
            if (total > Long.MAX_VALUE - pixels) Long.MAX_VALUE else total + pixels
        }
        return if (totalPixels > Long.MAX_VALUE / BYTES_PER_PIXEL) Long.MAX_VALUE else totalPixels * BYTES_PER_PIXEL
    }
}

/** The missing side is a complete difference; the source page stays owned by its caller. */
internal fun <T : Any> missingPageDiff(
    left: T?,
    right: T?,
    highlightCopy: (T) -> T
): Pair<T, Float>? {
    if ((left == null) == (right == null)) return null
    return highlightCopy((left ?: right)!!) to 100f
}

/** Translucent red keeps the surviving page readable while marking the whole page changed. */
internal val MISSING_PAGE_OVERLAY_COLOR = 0x88FF3232.toInt()

/** PdfDocument has close(), but does not implement Closeable on supported Android APIs.
 * Keep the rendering error primary if close() also rejects an unfinished page.
 */
internal inline fun <T, R> withClosePreservingPrimaryFailure(
    resource: T,
    close: (T) -> Unit,
    block: (T) -> R
): R {
    var primaryFailure: Throwable? = null
    try {
        return block(resource)
    } catch (failure: Throwable) {
        primaryFailure = failure
        throw failure
    } finally {
        try {
            close(resource)
        } catch (cleanupFailure: Throwable) {
            val primary = primaryFailure
            if (primary == null) throw cleanupFailure
            if (cleanupFailure !== primary) primary.addSuppressed(cleanupFailure)
        }
    }
}

/** Acquire the second resource only while the first is under a close-on-failure scope. */
internal inline fun <A : java.io.Closeable, B : java.io.Closeable, T> withOpenedCloseables(
    openFirst: () -> A,
    openSecond: () -> B,
    block: (A, B) -> T
): T = openFirst().use { first -> openSecond().use { second -> block(first, second) } }

/** ASCII names stay below common 255-byte component limits, including .pdf and collision suffixes. */
internal const val MAX_COMPARE_OUTPUT_BASE_NAME_LENGTH = 200

/** Preserve ordinary comparison names while bounding long user-provided names. */
internal fun sanitizedCompareOutputBaseName(
    outputName: String,
    fallback: () -> String = { "compare_${System.currentTimeMillis()}" }
): String {
    val input = outputName.ifBlank(fallback)
    val sanitized = StringBuilder(minOf(input.length, MAX_COMPARE_OUTPUT_BASE_NAME_LENGTH))
    var index = 0
    while (index < input.length && sanitized.length < MAX_COMPARE_OUTPUT_BASE_NAME_LENGTH) {
        val current = input[index]
        sanitized.append(
            if (current in 'a'..'z' || current in 'A'..'Z' ||
                current in '0'..'9' || current == '_' || current == '-'
            ) current else '_'
        )
        // Java Regex replaces a valid surrogate pair as one code point, not two chars.
        index += if (current.isHighSurrogate() && index + 1 < input.length &&
            input[index + 1].isLowSurrogate()
        ) 2 else 1
    }
    return sanitized.toString()
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
        val scale = normalizedCompareRenderScale(renderScale)

        context.withUriCopiedToCacheFile(leftUri, prefix = "docforge_cmp_left_", suffix = ".pdf") { leftFile ->
            context.withUriCopiedToCacheFile(rightUri, prefix = "docforge_cmp_right_", suffix = ".pdf") { rightFile ->
                withOpenedCloseables(
                    openFirst = { ParcelFileDescriptor.open(leftFile, ParcelFileDescriptor.MODE_READ_ONLY) },
                    openSecond = { ParcelFileDescriptor.open(rightFile, ParcelFileDescriptor.MODE_READ_ONLY) }
                ) { lPfd, rPfd ->
                        PdfRenderer(lPfd).use { leftRenderer ->
                            PdfRenderer(rPfd).use { rightRenderer ->
                                val pageCount = max(leftRenderer.pageCount, rightRenderer.pageCount)
                                require(pageCount > 0) { "Both PDFs have no pages." }

                                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                                    context = context,
                                    bucket = DocForgeOutputBucket.DOCUMENTS
                                )
                                val sanitized = sanitizedCompareOutputBaseName(outputName)
                                val stagedResult = withStagedOutputFile(
                                    directory = outputDir,
                                    baseName = sanitized,
                                    extension = "pdf"
                                ) { stagedFile ->
                                    val diffPcts = mutableListOf<Float>()
                                    withClosePreservingPrimaryFailure(
                                        resource = android.graphics.pdf.PdfDocument(),
                                        close = { it.close() }
                                    ) { pdfDoc ->
                                        for (pageIndex in 0 until pageCount) {
                                            checkCancelled()
                                            var leftBmp: Bitmap? = null
                                            var rightBmp: Bitmap? = null
                                            var diffBmp: Bitmap? = null
                                            withComparePageBitmapCleanup(
                                                resources = { listOf(leftBmp, rightBmp, diffBmp) },
                                                recycle = Bitmap::recycle
                                            ) {
                                                val leftPageSize = pageSize(leftRenderer, pageIndex)
                                                val rightPageSize = pageSize(rightRenderer, pageIndex)
                                                val (leftSize, rightSize) = CompareRasterBudget.fit(
                                                    left = leftPageSize?.let { scaledCompareRasterSize(it, scale) },
                                                    right = rightPageSize?.let { scaledCompareRasterSize(it, scale) },
                                                    budgetBytes = Runtime.getRuntime().let { runtime ->
                                                        CompareRasterBudget.heapAwareBudgetBytes(
                                                            maxHeapBytes = runtime.maxMemory(),
                                                            occupiedHeapBytes = runtime.totalMemory() - runtime.freeMemory()
                                                        )
                                                    }
                                                )
                                                leftBmp = renderPage(leftRenderer, pageIndex, leftSize)
                                                rightBmp = renderPage(rightRenderer, pageIndex, rightSize)

                                                val diff = diffBitmaps(leftBmp, rightBmp, threshold, checkCancelled)
                                                diffBmp = diff.first
                                                diffPcts += diff.second

                                                val outputPageSize = compareOutputPageSize(leftPageSize, rightPageSize)
                                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                                    outputPageSize.width,
                                                    outputPageSize.height,
                                                    pageIndex + 1
                                                ).create()
                                                val page = pdfDoc.startPage(pageInfo)
                                                page.canvas.drawBitmap(
                                                    diffBmp,
                                                    null,
                                                    android.graphics.Rect(0, 0, outputPageSize.width, outputPageSize.height),
                                                    null
                                                )
                                                pdfDoc.finishPage(page)
                                            }
                                        }

                                        FileOutputStream(stagedFile).use { fileStream ->
                                            pdfDoc.writeTo(CancellationCheckingOutputStream(fileStream, checkCancelled))
                                        }
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

    private fun pageSize(renderer: PdfRenderer, pageIndex: Int): CompareRasterSize? {
        if (pageIndex >= renderer.pageCount) return null
        renderer.openPage(pageIndex).use { page ->
            return CompareRasterSize(width = page.width, height = page.height)
        }
    }

    private fun renderPage(renderer: PdfRenderer, pageIndex: Int, size: CompareRasterSize?): Bitmap? {
        if (size == null || pageIndex >= renderer.pageCount) return null
        renderer.openPage(pageIndex).use { page ->
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            return renderComparePageWithCleanup(bitmap, Bitmap::recycle) { target ->
                target.eraseColor(Color.WHITE)
                page.render(target, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    /**
     * Returns a diff bitmap and the percentage of pixels that differ.
     * - Identical pixels → light grey
     * - Changed pixels → red overlay
     * - Missing page → entire page marked as red
     */
    private fun diffBitmaps(left: Bitmap?, right: Bitmap?, threshold: Int, checkCancelled: () -> Unit): Pair<Bitmap, Float> {
        if (left == null && right == null) {
            val blank = allocateCompareDiffRaster(checkCancelled) {
                Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
            }
            blank.eraseColor(Color.LTGRAY)
            return blank to 0f
        }
        val missingPage = missingPageDiff(left, right) { present ->
            checkCancelled()
            val highlighted = present.copy(Bitmap.Config.ARGB_8888, true)
                ?: error("Unable to copy PDF compare page")
            withCleanupOnFailure(highlighted, Bitmap::recycle) {
                checkCancelled()
                android.graphics.Canvas(highlighted).drawColor(MISSING_PAGE_OVERLAY_COLOR)
                checkCancelled()
                highlighted
            }
        }
        if (missingPage != null) return missingPage
        if (left == null || right == null) error("Missing-page diff was not handled")

        val w = max(left.width, right.width)
        val h = max(left.height, right.height)
        val output = allocateCompareDiffRaster(checkCancelled) {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }
        return withCleanupOnFailure(output, Bitmap::recycle) {
    
            var diffPixels = 0L
            val totalPixels = Math.multiplyExact(w.toLong(), h.toLong())
    
            // Three small row buffers avoid per-pixel Bitmap JNI calls without holding full rows.
            val chunkWidth = minOf(COMPARE_DIFF_ROW_CHUNK_PIXELS, w)
            val leftPixels = IntArray(chunkWidth)
            val rightPixels = IntArray(chunkWidth)
            val outputPixels = IntArray(chunkWidth)
            fun readChunk(source: Bitmap, pixels: IntArray, x: Int, y: Int, count: Int) {
                if (y >= source.height || x >= source.width) {
                    fillCompareMissingChunkPixels(pixels, 0, count)
                    return
                }
                val copied = minOf(count, source.width - x)
                source.getPixels(pixels, 0, copied, x, y, copied, 1)
                fillCompareMissingChunkPixels(pixels, copied, count)
            }

            forEachDiffRasterChunk(w, h, checkCancelled) { y, x, count ->
                readChunk(left, leftPixels, x, y, count)
                readChunk(right, rightPixels, x, y, count)
                diffPixels += compareDiffArgbChunk(leftPixels, rightPixels, outputPixels, count, threshold)
                output.setPixels(outputPixels, 0, count, x, y, count, 1)
            }

            val pct = if (totalPixels > 0) (diffPixels.toFloat() / totalPixels * 100f) else 0f
            output to pct
        }
    }
}

/** Avoid allocating an output bitmap after comparison has already been cancelled. */
internal inline fun <T> allocateCompareDiffRaster(
    checkCancelled: () -> Unit,
    allocate: () -> T
): T {
    checkCancelled()
    return allocate()
}

/** Attempt all page bitmap recycles and keep page-processing exceptions primary. */
internal fun <T, R> withComparePageBitmapCleanup(
    resources: () -> List<T?>,
    recycle: (T) -> Unit,
    block: () -> R
): R = withClosePreservingPrimaryFailure(
    resource = Unit,
    close = {
        var firstFailure: Throwable? = null
        for (resource in resources()) {
            if (resource != null) {
                try {
                    recycle(resource)
                } catch (failure: Throwable) {
                    val first = firstFailure
                    if (first == null) firstFailure = failure
                    else if (failure !== first) first.addSuppressed(failure)
                }
            }
        }
        firstFailure?.let { throw it }
    },
    block = { block() }
)

/** Rendering owns the bitmap until success; failed rendering recycles without masking its cause. */
internal inline fun <T> renderComparePageWithCleanup(
    bitmap: T,
    recycle: (T) -> Unit,
    render: (T) -> Unit
): T = withCleanupOnFailure(bitmap, recycle) {
    render(bitmap)
    bitmap
}

/** The caller owns [resource] only after [block] returns successfully. */
internal inline fun <T, R> withCleanupOnFailure(
    resource: T,
    cleanup: (T) -> Unit,
    block: () -> R
): R {
    try {
        return block()
    } catch (failure: Throwable) {
        try {
            cleanup(resource)
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
        }
        throw failure
    }
}

internal inline fun forEachDiffRow(
    height: Int,
    checkCancelled: () -> Unit,
    block: (Int) -> Unit
) {
    for (y in 0 until height) {
        checkCancelled()
        block(y)
    }
}

/** Keep cancellation responsive even when a single PDF raster row is extremely wide. */
internal inline fun forEachDiffColumn(
    width: Int,
    checkCancelled: () -> Unit,
    block: (Int) -> Unit
) {
    for (x in 0 until width) {
        if (x % 4096 == 0) checkCancelled()
        block(x)
    }
}

/** White-fill only pixels absent from the source; getPixels already overwrites the prefix. */
internal fun fillCompareMissingChunkPixels(pixels: IntArray, copied: Int, count: Int) {
    if (copied < count) {
        java.util.Arrays.fill(pixels, copied, count, 0xFFFFFFFF.toInt())
    }
}

/** Limit transient row buffers to 12 KiB total for the two inputs and diff output. */
internal const val COMPARE_DIFF_ROW_CHUNK_PIXELS = 1024

/** Check cancellation at the same 4096-column boundaries as the original pixel loop. */
internal inline fun forEachDiffChunk(
    width: Int,
    checkCancelled: () -> Unit,
    block: (start: Int, count: Int) -> Unit
) {
    var start = 0
    while (start < width) {
        if (start % 4096 == 0) checkCancelled()
        val count = minOf(COMPARE_DIFF_ROW_CHUNK_PIXELS, width - start)
        block(start, count)
        start += count
    }
}

/** The normal two-page diff writes every output pixel before returning its bitmap. */
internal inline fun forEachDiffRasterChunk(
    width: Int,
    height: Int,
    checkCancelled: () -> Unit,
    block: (row: Int, start: Int, count: Int) -> Unit
) {
    forEachDiffRow(height, checkCancelled) { row ->
        forEachDiffChunk(width, checkCancelled) { start, count ->
            block(row, start, count)
        }
    }
}

/** Pure JVM-testable ARGB comparison. Alpha is intentionally ignored, as in the old loop. */
internal fun compareDiffArgbChunk(
    left: IntArray,
    right: IntArray,
    output: IntArray,
    count: Int,
    threshold: Int
): Int {
    var changed = 0
    for (index in 0 until count) {
        val leftArgb = left[index]
        val rightArgb = right[index]
        val leftRed = (leftArgb ushr 16) and 255
        val leftGreen = (leftArgb ushr 8) and 255
        val leftBlue = leftArgb and 255
        val rightRed = (rightArgb ushr 16) and 255
        val rightGreen = (rightArgb ushr 8) and 255
        val rightBlue = rightArgb and 255

        if (abs(leftRed - rightRed) > threshold ||
            abs(leftGreen - rightGreen) > threshold ||
            abs(leftBlue - rightBlue) > threshold
        ) {
            output[index] = 0xC8FF3232.toInt()
            changed++
        } else {
            val grey = (leftRed * 0.3f + leftGreen * 0.59f + leftBlue * 0.11f).toInt()
            output[index] = 0xFF000000.toInt() or (grey shl 16) or (grey shl 8) or grey
        }
    }
    return changed
}
