package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.sqrt

enum class PdfPageImageFormat {
    JPG,
    PNG,
    WEBP
}

data class PdfPageImageExportResult(
    val outputFiles: List<File>,
    val outputSizeBytes: Long,
    val pageCount: Int,
    val bundleZipFile: File? = null
)

class PdfPageImageExporter(
    private val context: Context
) {

    suspend fun exportPages(
        inputUri: Uri,
        outputBaseName: String,
        format: PdfPageImageFormat,
        jpegQuality: Int = 90,
        scaleFactor: Float = 2.5f,
        zipBundle: Boolean = false
    ): PdfPageImageExportResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_pageimg_src_", suffix = ".pdf") { sourceFile ->
            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    require(renderer.pageCount > 0) { "Input PDF has no pages." }

                    val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                        context = context,
                        bucket = DocForgeOutputBucket.DOCUMENTS
                    )

                    val base = outputBaseName.ifBlank { "pdf_pages_${System.currentTimeMillis()}" }
                        .replace(Regex("[^a-zA-Z0-9_-]"), "_")

                    val bitmapBudgetBytes = pdfPageImageBitmapBudgetBytes(Runtime.getRuntime().maxMemory())
                    val ext = when (format) {
                        PdfPageImageFormat.JPG -> "jpg"
                        PdfPageImageFormat.PNG -> "png"
                        PdfPageImageFormat.WEBP -> "webp"
                    }

                    if (zipBundle) {
                        val stagedZip = withStagedOutputFile(
                            directory = outputDir,
                            baseName = "${base}_${format.name.lowercase()}_bundle",
                            extension = "zip"
                        ) { stagedFile ->
                            ZipOutputStream(FileOutputStream(stagedFile)).use { zipOut ->
                                repeat(renderer.pageCount) { pageIndex ->
                                    checkCancelled()
                                    zipOut.putNextEntry(ZipEntry("${base}_p${pageIndex + 1}.$ext"))
                                    try {
                                        renderPageImage(
                                            renderer = renderer,
                                            pageIndex = pageIndex,
                                            format = format,
                                            jpegQuality = jpegQuality,
                                            scaleFactor = scaleFactor,
                                            bitmapBudgetBytes = bitmapBudgetBytes,
                                            output = zipOut
                                        )
                                    } finally {
                                        zipOut.closeEntry()
                                    }
                                }
                            }
                            Unit
                        }

                        return@withUriCopiedToCacheFile PdfPageImageExportResult(
                            outputFiles = emptyList(),
                            outputSizeBytes = stagedZip.outputFile.length(),
                            pageCount = renderer.pageCount,
                            bundleZipFile = stagedZip.outputFile
                        )
                    }

                    val pageRequests = (0 until renderer.pageCount).map { pageIndex ->
                        StagedOutputRequest(
                            baseName = "${base}_p${pageIndex + 1}",
                            extension = ext
                        ) { stagedFile ->
                            checkCancelled()
                            FileOutputStream(stagedFile).use { stream ->
                                renderPageImage(
                                    renderer = renderer,
                                    pageIndex = pageIndex,
                                    format = format,
                                    jpegQuality = jpegQuality,
                                    scaleFactor = scaleFactor,
                                    bitmapBudgetBytes = bitmapBudgetBytes,
                                    output = stream
                                )
                            }
                            Unit
                        }
                    }

                    val stagedPages = withStagedOutputFiles(
                        directory = outputDir,
                        requests = pageRequests
                    )
                    val files = stagedPages.map { it.outputFile }

                    PdfPageImageExportResult(
                        outputFiles = files,
                        outputSizeBytes = files.sumOf(File::length),
                        pageCount = renderer.pageCount,
                        bundleZipFile = null
                    )
                }
            }
        }
    }

    private fun renderPageImage(
        renderer: PdfRenderer,
        pageIndex: Int,
        format: PdfPageImageFormat,
        jpegQuality: Int,
        scaleFactor: Float,
        bitmapBudgetBytes: Long,
        output: OutputStream
    ) {
        renderer.openPage(pageIndex).use { page ->
            val rasterSize = boundedPdfPageImageRasterSize(
                pageWidthPoints = page.width,
                pageHeightPoints = page.height,
                scaleFactor = scaleFactor,
                maxBitmapBytes = bitmapBudgetBytes
            )
            val bitmap = Bitmap.createBitmap(
                rasterSize.width,
                rasterSize.height,
                Bitmap.Config.ARGB_8888
            )
            try {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                val ok = bitmap.compress(
                    format.toBitmapCompressFormat(),
                    jpegQuality.coerceIn(10, 100),
                    output
                )
                require(ok) { "Failed to encode page ${pageIndex + 1}." }
            } finally {
                bitmap.recycle()
            }
        }
    }

    suspend fun getPageCount(inputUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_pagecount_src_", suffix = ".pdf") { sourceFile ->
                ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
                }
            }
        }.getOrDefault(0)
    }
}

private fun PdfPageImageFormat.toBitmapCompressFormat(): Bitmap.CompressFormat {
    return when (this) {
        PdfPageImageFormat.JPG -> Bitmap.CompressFormat.JPEG
        PdfPageImageFormat.PNG -> Bitmap.CompressFormat.PNG
        PdfPageImageFormat.WEBP -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
    }
}


private const val PDF_PAGE_IMAGE_BYTES_PER_PIXEL = 4L
private const val PDF_PAGE_IMAGE_MIN_BITMAP_BUDGET_BYTES = 8L * 1024L * 1024L
private const val PDF_PAGE_IMAGE_MAX_BITMAP_BUDGET_BYTES = 32L * 1024L * 1024L

internal fun pdfPageImageBitmapBudgetBytes(maxHeapBytes: Long): Long {
    val heapAwareBudget = (maxHeapBytes / 8L).coerceAtLeast(1L)
    return heapAwareBudget.coerceIn(
        PDF_PAGE_IMAGE_MIN_BITMAP_BUDGET_BYTES,
        PDF_PAGE_IMAGE_MAX_BITMAP_BUDGET_BYTES
    )
}

internal fun boundedPdfPageImageRasterSize(
    pageWidthPoints: Int,
    pageHeightPoints: Int,
    scaleFactor: Float,
    maxBitmapBytes: Long
): PdfRasterSize {
    require(pageWidthPoints > 0 && pageHeightPoints > 0) { "PDF page dimensions must be positive." }
    require(scaleFactor.isFinite() && scaleFactor > 0f) { "Scale factor must be finite and positive." }
    require(maxBitmapBytes >= PDF_PAGE_IMAGE_BYTES_PER_PIXEL) { "Bitmap budget is too small." }

    val desiredWidth = (pageWidthPoints.toDouble() * scaleFactor.toDouble())
        .coerceIn(1.0, Int.MAX_VALUE.toDouble())
        .toInt()
        .coerceAtLeast(1)
    val desiredHeight = (pageHeightPoints.toDouble() * scaleFactor.toDouble())
        .coerceIn(1.0, Int.MAX_VALUE.toDouble())
        .toInt()
        .coerceAtLeast(1)

    val maxPixels = (maxBitmapBytes / PDF_PAGE_IMAGE_BYTES_PER_PIXEL).coerceAtLeast(1L)
    val desiredPixels = desiredWidth.toDouble() * desiredHeight.toDouble()
    if (desiredPixels <= maxPixels.toDouble()) {
        return PdfRasterSize(desiredWidth, desiredHeight)
    }

    val downscale = sqrt(maxPixels.toDouble() / desiredPixels)
    var width = (desiredWidth.toDouble() * downscale).toInt().coerceAtLeast(1)
    var height = (desiredHeight.toDouble() * downscale).toInt().coerceAtLeast(1)

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

    return PdfRasterSize(width.coerceAtLeast(1), height.coerceAtLeast(1))
}
