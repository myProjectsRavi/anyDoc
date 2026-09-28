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
import java.io.FileInputStream
import java.io.FileOutputStream
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

                    val files = mutableListOf<File>()
                    var totalBytes = 0L
                    val bitmapBudgetBytes = pdfPageImageBitmapBudgetBytes(Runtime.getRuntime().maxMemory())

                    repeat(renderer.pageCount) { pageIndex ->
                        checkCancelled()
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
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            val ext = when (format) {
                                PdfPageImageFormat.JPG -> "jpg"
                                PdfPageImageFormat.PNG -> "png"
                                PdfPageImageFormat.WEBP -> "webp"
                            }
                            val file = resolveNonConflictingFile(outputDir, "${base}_p${pageIndex + 1}", ext)

                            try {
                                FileOutputStream(file).use { stream ->
                                    val ok = bitmap.compress(format.toBitmapCompressFormat(), jpegQuality.coerceIn(10, 100), stream)
                                    require(ok) { "Failed to encode page ${pageIndex + 1}." }
                                }
                                files += file
                                totalBytes += file.length()
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }

                    val zipFile = if (zipBundle) {
                        val zip = resolveNonConflictingFile(outputDir, "${base}_${format.name.lowercase()}_bundle", "zip")
                        ZipOutputStream(FileOutputStream(zip)).use { zipOut ->
                            files.forEach { imageFile ->
                                checkCancelled()
                                FileInputStream(imageFile).use { input ->
                                    zipOut.putNextEntry(ZipEntry(imageFile.name))
                                    input.copyTo(zipOut, bufferSize = 8 * 1024)
                                    zipOut.closeEntry()
                                }
                            }
                        }
                        // Delete individual files — ZIP contains them all
                        files.forEach { it.delete() }
                        zip
                    } else {
                        null
                    }

                    PdfPageImageExportResult(
                        outputFiles = if (zipFile != null) emptyList() else files,
                        outputSizeBytes = zipFile?.length() ?: totalBytes,
                        pageCount = renderer.pageCount,
                        bundleZipFile = zipFile
                    )
                }
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

    while (width.toLong() * height.toLong() > maxPixels) {
        if (width >= height && width > 1) {
            width -= 1
        } else if (height > 1) {
            height -= 1
        } else {
            break
        }
    }

    return PdfRasterSize(width, height)
}
