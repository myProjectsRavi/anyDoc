package com.docforge.core.pdf

import android.content.Context
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfTextExtractionResult(
    val outputFile: File,
    val outputSizeBytes: Long,
    val extractedChars: Int,
    val pageCount: Int
)

class PdfTextExtractor(
    private val context: Context
) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun extractToTxt(
        inputUri: android.net.Uri,
        outputName: String
    ): PdfTextExtractionResult = withContext(Dispatchers.IO) {
        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_extract_", suffix = ".pdf") { sourceFile ->
            PDDocument.load(sourceFile).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = 1
                    endPage = document.numberOfPages
                }

                val rawText = stripper.getText(document)
                val extracted = if (rawText.isBlank()) {
                    "[No extractable text found in this PDF.]"
                } else {
                    rawText
                }

                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                    context = context,
                    bucket = DocForgeOutputBucket.DOCUMENTS
                )

                val sanitized = outputName.ifBlank { "pdf_text_${System.currentTimeMillis()}" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val output = File(outputDir, "$sanitized.txt")

                FileOutputStream(output).use { stream ->
                    stream.write(extracted.toByteArray())
                }

                PdfTextExtractionResult(
                    outputFile = output,
                    outputSizeBytes = output.length(),
                    extractedChars = extracted.length,
                    pageCount = document.numberOfPages
                )
            }
        }
    }

    suspend fun getPageCount(inputUri: android.net.Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_count_", suffix = ".pdf") { sourceFile ->
                PDDocument.load(sourceFile).use { document ->
                    document.numberOfPages
                }
            }
        }.getOrDefault(0)
    }
}
