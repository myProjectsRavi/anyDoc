package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

enum class PdfCompressionLevel(
    val scaleFactor: Float,
    val estimatedRatio: Float
) {
    HIGH(scaleFactor = 1.0f, estimatedRatio = 0.95f),
    MEDIUM(scaleFactor = 0.9f, estimatedRatio = 0.85f),
    LOW(scaleFactor = 0.8f, estimatedRatio = 0.75f)
}

class PdfCompressor(
    private val context: Context
) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun compress(
        inputUri: Uri,
        outputName: String,
        level: PdfCompressionLevel
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "compressed_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(outputDir, "$sanitized.pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_compress_src_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { sourceDoc ->
                require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

                PDDocument().use { outDoc ->
                    repeat(sourceDoc.numberOfPages) { pageIndex ->
                        checkCancelled()
                        importPage(outDoc, sourceDoc.getPage(pageIndex))
                    }

                    // Keep content vector-native and avoid destructive raster compression.
                    applyCompressionProfile(outDoc, level)
                    outDoc.save(outputFile)
                }

                PdfCreationResult(
                    outputFile = outputFile,
                    pageCount = sourceDoc.numberOfPages,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    suspend fun getPageCount(inputUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_compress_count_", suffix = ".pdf") { sourceFile ->
                loadPdfDocument(sourceFile).use { sourceDoc -> sourceDoc.numberOfPages }
            }
        }.getOrDefault(0)
    }

    private fun importPage(outDoc: PDDocument, sourcePage: PDPage): PDPage {
        val imported = outDoc.importPage(sourcePage)
        imported.rotation = sourcePage.rotation
        imported.mediaBox = sourcePage.mediaBox
        imported.cropBox = sourcePage.cropBox
        imported.resources = sourcePage.resources
        return imported
    }

    private fun applyCompressionProfile(document: PDDocument, level: PdfCompressionLevel) {
        when (level) {
            PdfCompressionLevel.HIGH -> Unit
            PdfCompressionLevel.MEDIUM -> {
                document.documentCatalog.metadata = null
            }
            PdfCompressionLevel.LOW -> {
                document.documentCatalog.metadata = null
                val info = document.documentInformation
                info.author = null
                info.subject = null
                info.creator = null
                info.keywords = null
                info.producer = null
                document.documentInformation = info
            }
        }
    }
}
