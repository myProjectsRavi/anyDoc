package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Adds header text, footer text, and/or page numbers to every page of a PDF.
 *
 * Sprint 4 feature — PDF Header / Footer / Page Numbers.
 */
class PdfHeaderFooterTool(
    private val context: Context
) {

    /**
     * Adds text overlays to every page.
     *
     * @param headerText  optional text centred at the top of each page.
     * @param footerText  optional text centred at the bottom of each page.
     * @param showPageNumbers if `true`, "Page X of N" is placed at the bottom-right corner.
     * @param fontSize     font size in points (default 10).
     * @param marginPt     margin from page edge in points (default 36 ≈ 0.5 in).
     */
    suspend fun addHeaderFooter(
        inputUri: Uri,
        outputName: String,
        headerText: String? = null,
        footerText: String? = null,
        showPageNumbers: Boolean = true,
        fontSize: Float = 10f,
        marginPt: Float = 36f
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(
            !headerText.isNullOrBlank() || !footerText.isNullOrBlank() || showPageNumbers
        ) { "Provide at least one of: header text, footer text, or page numbers." }

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_hf_src_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                val totalPages = document.numberOfPages
                require(totalPages > 0) { "Input PDF has no pages." }

                val font = PDType1Font.HELVETICA
                val safeFontSize = fontSize.coerceIn(6f, 36f)

                for (i in 0 until totalPages) {
                    coroutineContext.ensureActive()

                    val page = document.getPage(i)
                    val mediaBox = page.mediaBox
                    val pageWidth = mediaBox.width
                    val pageHeight = mediaBox.height

                    PDPageContentStream(
                        document, page,
                        PDPageContentStream.AppendMode.APPEND,
                        true, true
                    ).use { cs ->
                        cs.setFont(font, safeFontSize)

                        // Header — centred
                        if (!headerText.isNullOrBlank()) {
                            val textWidth = font.getStringWidth(headerText) / 1000f * safeFontSize
                            val x = (pageWidth - textWidth) / 2f
                            val y = pageHeight - marginPt
                            cs.beginText()
                            cs.newLineAtOffset(x, y)
                            cs.showText(headerText)
                            cs.endText()
                        }

                        // Footer — centred
                        if (!footerText.isNullOrBlank()) {
                            val textWidth = font.getStringWidth(footerText) / 1000f * safeFontSize
                            val x = (pageWidth - textWidth) / 2f
                            val y = marginPt
                            cs.beginText()
                            cs.newLineAtOffset(x, y)
                            cs.showText(footerText)
                            cs.endText()
                        }

                        // Page numbers — bottom-right
                        if (showPageNumbers) {
                            val label = "Page ${i + 1} of $totalPages"
                            val textWidth = font.getStringWidth(label) / 1000f * safeFontSize
                            val x = pageWidth - marginPt - textWidth
                            val y = marginPt
                            cs.beginText()
                            cs.newLineAtOffset(x, y)
                            cs.showText(label)
                            cs.endText()
                        }
                    }
                }

                val outputFile = resolveOutput(outputName, "headerfooter")
                document.save(outputFile)

                PdfCreationResult(
                    outputFile = outputFile,
                    pageCount = totalPages,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    private fun resolveOutput(outputName: String, fallback: String): File {
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "${fallback}_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return resolveNonConflictingFile(outputDir, sanitized, "pdf")
    }
}
