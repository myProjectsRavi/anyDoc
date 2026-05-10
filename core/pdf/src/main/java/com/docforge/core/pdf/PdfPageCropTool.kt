package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Crops PDF pages by adjusting the crop box / media box.
 *
 * Sprint 4 feature — PDF Page Crop tool.
 */
class PdfPageCropTool(
    private val context: Context
) {

    /**
     * Crops every page of the PDF by the specified margin percentages (0.0–0.5 each).
     *
     * @param leftPct   fraction of page width to remove from the left   (0.0 = no crop, 0.25 = 25 %)
     * @param topPct    fraction of page height to remove from the top
     * @param rightPct  fraction of page width to remove from the right
     * @param bottomPct fraction of page height to remove from the bottom
     */
    suspend fun cropAllPages(
        inputUri: Uri,
        outputName: String,
        leftPct: Float = 0f,
        topPct: Float = 0f,
        rightPct: Float = 0f,
        bottomPct: Float = 0f
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        requireValidPercent(leftPct, "leftPct")
        requireValidPercent(topPct, "topPct")
        requireValidPercent(rightPct, "rightPct")
        requireValidPercent(bottomPct, "bottomPct")
        require(leftPct + rightPct < 1f) { "Left + right crop exceeds 100 %." }
        require(topPct + bottomPct < 1f) { "Top + bottom crop exceeds 100 %." }

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_crop_src_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                require(document.numberOfPages > 0) { "Input PDF has no pages." }

                for (i in 0 until document.numberOfPages) {
                    coroutineContext.ensureActive()
                    val page = document.getPage(i)
                    val media = page.mediaBox ?: PDRectangle.A4
                    val w = media.width
                    val h = media.height

                    page.cropBox = PDRectangle(
                        media.lowerLeftX + w * leftPct,
                        media.lowerLeftY + h * bottomPct,
                        w * (1f - leftPct - rightPct),
                        h * (1f - topPct - bottomPct)
                    )
                }

                val outputFile = outputFile(document, outputName, "cropped")
                document.save(outputFile)

                PdfCreationResult(
                    outputFile = outputFile,
                    pageCount = document.numberOfPages,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    /**
     * Crops specific pages by absolute point values.
     *
     * @param pagesToCropOneBased map of page number → [CropInsets] in PDF points.
     */
    suspend fun cropPages(
        inputUri: Uri,
        outputName: String,
        pagesToCropOneBased: Map<Int, CropInsets>
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(pagesToCropOneBased.isNotEmpty()) { "No pages specified for cropping." }

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_crop_src_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                require(document.numberOfPages > 0) { "Input PDF has no pages." }

                pagesToCropOneBased.forEach { (pageOneBased, insets) ->
                    coroutineContext.ensureActive()
                    require(pageOneBased in 1..document.numberOfPages) { "Page $pageOneBased out of bounds." }

                    val page = document.getPage(pageOneBased - 1)
                    val media = page.mediaBox ?: PDRectangle.A4

                    val newLx = (media.lowerLeftX + insets.left).coerceAtMost(media.upperRightX - 1f)
                    val newLy = (media.lowerLeftY + insets.bottom).coerceAtMost(media.upperRightY - 1f)
                    val newW = (media.width - insets.left - insets.right).coerceAtLeast(1f)
                    val newH = (media.height - insets.top - insets.bottom).coerceAtLeast(1f)

                    page.cropBox = PDRectangle(newLx, newLy, newW, newH)
                }

                val outputFile = outputFile(document, outputName, "cropped")
                document.save(outputFile)

                PdfCreationResult(
                    outputFile = outputFile,
                    pageCount = document.numberOfPages,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    private fun outputFile(document: PDDocument, outputName: String, fallback: String): File {
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "${fallback}_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return resolveNonConflictingFile(outputDir, sanitized, "pdf")
    }

    private fun requireValidPercent(value: Float, name: String) {
        require(value in 0f..0.5f) { "$name must be between 0.0 and 0.5, got $value." }
    }
}

/**
 * Crop insets in PDF points (1/72 inch).
 */
data class CropInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)
