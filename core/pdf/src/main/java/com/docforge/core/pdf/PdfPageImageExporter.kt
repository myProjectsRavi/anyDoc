package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
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
        scaleFactor: Float = 1f,
        zipBundle: Boolean = false
    ): PdfPageImageExportResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        context.contentResolver.openFileDescriptor(inputUri, "r")?.use { descriptor ->
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

                repeat(renderer.pageCount) { pageIndex ->
                    checkCancelled()
                    renderer.openPage(pageIndex).use { page ->
                        val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                        val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val ext = when (format) {
                            PdfPageImageFormat.JPG -> "jpg"
                            PdfPageImageFormat.PNG -> "png"
                            PdfPageImageFormat.WEBP -> "webp"
                        }
                        val file = File(outputDir, "${base}_p${pageIndex + 1}.$ext")

                        FileOutputStream(file).use { stream ->
                            val ok = bitmap.compress(format.toBitmapCompressFormat(), jpegQuality.coerceIn(10, 100), stream)
                            require(ok) { "Failed to encode page ${pageIndex + 1}." }
                        }
                        bitmap.recycle()

                        files += file
                        totalBytes += file.length()
                    }
                }

                val zipFile = if (zipBundle) {
                    val zip = File(outputDir, "${base}_${format.name.lowercase()}_bundle.zip")
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
                    zip
                } else {
                    null
                }

                PdfPageImageExportResult(
                    outputFiles = files,
                    outputSizeBytes = totalBytes + (zipFile?.length() ?: 0L),
                    pageCount = renderer.pageCount,
                    bundleZipFile = zipFile
                )
            }
        } ?: error("Unable to open input PDF")
    }

    suspend fun getPageCount(inputUri: Uri): Int = withContext(Dispatchers.IO) {
        context.contentResolver.openFileDescriptor(inputUri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
        } ?: 0
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
