package com.docforge.core.pdf

import android.content.Context
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Writer

data class PdfTextExtractionResult(
    val outputFile: File,
    val outputSizeBytes: Long,
    val extractedChars: Int,
    val pageCount: Int
)

class PdfTextExtractor(
    private val context: Context
) {

    suspend fun extractToTxt(
        inputUri: android.net.Uri,
        outputName: String
    ): PdfTextExtractionResult = withContext(Dispatchers.IO) {
        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_extract_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = 1
                    endPage = document.numberOfPages
                }

                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                    context = context,
                    bucket = DocForgeOutputBucket.DOCUMENTS
                )

                val sanitized = outputName.ifBlank { "pdf_text_${System.currentTimeMillis()}" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val published = withStagedOutputFile(
                    directory = outputDir,
                    baseName = sanitized,
                    extension = "txt"
                ) { stagedFile ->
                    writeExtractedPdfText(stagedFile) { writer ->
                        stripper.writeText(document, writer)
                    }
                }

                PdfTextExtractionResult(
                    outputFile = published.outputFile,
                    outputSizeBytes = published.outputFile.length(),
                    extractedChars = published.value,
                    pageCount = document.numberOfPages
                )
            }
        }
    }

    suspend fun getPageCount(inputUri: android.net.Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_count_", suffix = ".pdf") { sourceFile ->
                loadPdfDocument(sourceFile).use { document ->
                    document.numberOfPages
                }
            }
        }.getOrDefault(0)
    }
}

private const val NO_EXTRACTABLE_TEXT = "[No extractable text found in this PDF.]"

/**
 * Streams PDFBox output to the staging file. Only the small empty-text fallback
 * is held in memory; extracted document text is never accumulated as a String.
 */
internal fun writeExtractedPdfText(stagedFile: File, extract: (Writer) -> Unit): Int {
    var extractedChars = 0
    var hasNonWhitespace = false
    FileOutputStream(stagedFile).bufferedWriter(Charsets.UTF_8).use { output ->
        val counting = CountingPdfTextWriter(output)
        extract(counting)
        counting.flush()
        extractedChars = counting.characterCount
        hasNonWhitespace = counting.hasNonWhitespace
    }
    if (!hasNonWhitespace) {
        FileOutputStream(stagedFile, false).bufferedWriter(Charsets.UTF_8).use { output ->
            output.write(NO_EXTRACTABLE_TEXT)
        }
        return NO_EXTRACTABLE_TEXT.length
    }
    return extractedChars
}

/** Character counts follow String.length semantics (UTF-16 code units). */
internal fun checkedExtractedCharCount(current: Int, additional: Int): Int {
    require(current >= 0 && additional >= 0) { "Character counts must be non-negative." }
    if (additional > Int.MAX_VALUE - current) {
        throw IOException("Extracted text exceeds the supported character count.")
    }
    return current + additional
}

private class CountingPdfTextWriter(private val output: Writer) : Writer() {
    var characterCount: Int = 0
        private set
    var hasNonWhitespace: Boolean = false
        private set

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        if (off < 0 || len < 0 || off > cbuf.size - len) throw IndexOutOfBoundsException()
        val next = checkedExtractedCharCount(characterCount, len)
        output.write(cbuf, off, len)
        characterCount = next
        if (!hasNonWhitespace) {
            for (index in off until off + len) {
                if (!cbuf[index].isWhitespace()) {
                    hasNonWhitespace = true
                    break
                }
            }
        }
    }

    override fun write(str: String, off: Int, len: Int) {
        if (off < 0 || len < 0 || off > str.length - len) throw IndexOutOfBoundsException()
        val next = checkedExtractedCharCount(characterCount, len)
        output.write(str, off, len)
        characterCount = next
        if (!hasNonWhitespace) {
            for (index in off until off + len) {
                if (!str[index].isWhitespace()) {
                    hasNonWhitespace = true
                    break
                }
            }
        }
    }

    override fun write(c: Int) {
        val next = checkedExtractedCharCount(characterCount, 1)
        output.write(c)
        characterCount = next
        if (!c.toChar().isWhitespace()) hasNonWhitespace = true
    }

    override fun flush() = output.flush()
    override fun close() = output.close()
}
