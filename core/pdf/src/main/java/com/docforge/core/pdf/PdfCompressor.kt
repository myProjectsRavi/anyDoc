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
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

enum class PdfCompressionLevel(
    /** DPI to render source pages at for rasterization-based compression */
    val renderDpi: Int,
    /** JPEG quality 0-100 for embedded page images */
    val jpegQuality: Int,
    /** Estimated output-to-input size ratio (for UI hint) */
    val estimatedRatio: Float
) {
    HIGH(renderDpi = 150, jpegQuality = 82, estimatedRatio = 0.65f),
    MEDIUM(renderDpi = 120, jpegQuality = 70, estimatedRatio = 0.45f),
    LOW(renderDpi = 96, jpegQuality = 58, estimatedRatio = 0.30f)
}

class PdfCompressor(
    private val context: Context
) {

    /**
     * @param onProgress called with (completedPages, totalPages) after each page is compressed.
     */
    suspend fun compress(
        inputUri: Uri,
        outputName: String,
        level: PdfCompressionLevel,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        PdfBoxInit.ensure(context)
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "compressed_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_compress_src_", suffix = ".pdf") { sourceFile ->
            val pageCount = compressWithJpegRasterization(
                sourceFile = sourceFile,
                outputFile = outputFile,
                renderDpi = level.renderDpi,
                jpegQuality = level.jpegQuality,
                onProgress = onProgress
            )

            PdfCreationResult(
                outputFile = outputFile,
                pageCount = pageCount,
                outputSizeBytes = outputFile.length()
            )
        }
    }

    suspend fun getPageCount(inputUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_compress_count_", suffix = ".pdf") { sourceFile ->
                loadPdfDocument(sourceFile).use { sourceDoc -> sourceDoc.numberOfPages }
            }
        }.getOrDefault(0)
    }

    /**
     * Rasterizes each page to a JPEG-embedded PDF using Android PdfRenderer + PdfBox.
     * This achieves real file size reduction, especially for scanned documents.
     * Trade-off: text is no longer selectable in the output.
     *
     * @return page count of the resulting document
     */
    private suspend fun compressWithJpegRasterization(
        sourceFile: File,
        outputFile: File,
        renderDpi: Int,
        jpegQuality: Int,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        val scale = renderDpi / 72f  // PDF native unit = 1/72 inch
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageCount = 0

        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                pageCount = renderer.pageCount
                require(pageCount > 0) { "Input PDF has no pages." }

                PDDocument().use { outDoc ->
                    for (i in 0 until renderer.pageCount) {
                        checkCancelled()
                        renderer.openPage(i).use { page ->
                            val bitmapWidth = (page.width * scale).roundToInt().coerceAtLeast(1)
                            val bitmapHeight = (page.height * scale).roundToInt().coerceAtLeast(1)

                            // Use RGB_565 for smaller memory footprint (no alpha needed for documents)
                            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.RGB_565)
                            try {
                                // Paint white background before rendering (PDF pages default to white)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                // Create a PdfBox page with original PDF point dimensions
                                val pdPage = PDPage(PDRectangle(page.width.toFloat(), page.height.toFloat()))
                                outDoc.addPage(pdPage)

                                // Embed JPEG-compressed image
                                val jpegImage = JPEGFactory.createFromImage(
                                    outDoc,
                                    bitmap,
                                    jpegQuality / 100f
                                )

                                PDPageContentStream(outDoc, pdPage).use { stream ->
                                    stream.drawImage(
                                        jpegImage,
                                        0f, 0f,
                                        page.width.toFloat(),
                                        page.height.toFloat()
                                    )
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                        onProgress?.invoke(i + 1, renderer.pageCount)
                    }

                    outDoc.save(outputFile)
                }
            }
        }

        pageCount
    }
}

