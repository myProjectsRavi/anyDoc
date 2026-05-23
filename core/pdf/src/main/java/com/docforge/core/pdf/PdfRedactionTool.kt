package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.contentstream.operator.Operator
import com.tom_roush.pdfbox.contentstream.operator.OperatorName
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSBase
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSString
import com.tom_roush.pdfbox.pdfparser.PDFStreamParser
import com.tom_roush.pdfbox.pdfwriter.ContentStreamWriter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDStream
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class PdfRedactionOptions(
    val terms: List<String>,
    val caseSensitive: Boolean = false,
    val scrubMetadata: Boolean = true,
    val scrubFormValues: Boolean = true,
    val verifyIrreversible: Boolean = true,
    val autoDetectPii: Boolean = false
)

data class PdfRedactionProgress(
    val stage: String,
    val current: Int,
    val total: Int
)

data class PdfRedactionResult(
    val outputFile: File,
    val pageCount: Int,
    val removedTextOperatorCount: Int,
    val clearedFormFieldCount: Int,
    val outputSizeBytes: Long
)

class PdfRedactionTool(
    private val context: Context
) {
    suspend fun redact(
        inputUri: Uri,
        outputName: String,
        options: PdfRedactionOptions,
        pageProgressChunk: Int = 2,
        onProgress: ((PdfRedactionProgress) -> Unit)? = null
    ): PdfRedactionResult = withContext(Dispatchers.IO) {
        val userTerms = options.terms
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        require(userTerms.isNotEmpty() || options.autoDetectPii) {
            "Provide at least one term to redact, or enable auto-detect PII."
        }

        val progressChunk = pageProgressChunk.coerceAtLeast(1)
        val coroutineCtx = currentCoroutineContext()

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_redact_src_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                document.setAllSecurityToBeRemoved(true)
                val pageCount = document.numberOfPages
                require(pageCount > 0) { "Input PDF has no pages." }

                // Build effective terms list (user-provided + auto-detected PII).
                val terms: List<String> = if (options.autoDetectPii) {
                    onProgress?.invoke(PdfRedactionProgress(stage = "Detecting sensitive data", current = 0, total = pageCount))
                    val extractedText = runCatching {
                        PDFTextStripper().getText(document)
                    }.getOrDefault("")
                    val auto = detectPiiTerms(extractedText)
                    (userTerms + auto).distinct()
                } else {
                    userTerms
                }

                require(terms.isNotEmpty()) {
                    "Auto-detect found no emails, phone numbers, or SSNs in this PDF."
                }

                onProgress?.invoke(PdfRedactionProgress(stage = "Redacting content streams", current = 0, total = pageCount))

                var removedTextOps = 0
                repeat(pageCount) { pageIndex ->
                    coroutineCtx.ensureActive()
                    val page = document.getPage(pageIndex)
                    removedTextOps += redactPageTextOperators(
                        document = document,
                        page = page,
                        terms = terms,
                        caseSensitive = options.caseSensitive
                    )

                    val processed = pageIndex + 1
                    if (processed == pageCount || processed % progressChunk == 0) {
                        onProgress?.invoke(
                            PdfRedactionProgress(
                                stage = "Redacting content streams",
                                current = processed,
                                total = pageCount
                            )
                        )
                    }
                }

                val clearedFormFieldCount = if (options.scrubFormValues) {
                    clearMatchingFormValues(document = document, terms = terms, caseSensitive = options.caseSensitive)
                } else {
                    0
                }

                if (options.scrubMetadata) {
                    scrubMetadata(document)
                }
                // Scrub annotations on every page that may contain redaction terms
                scrubAnnotations(document, terms, options.caseSensitive)

                // Remove embedded files that could contain recoverable content
                scrubEmbeddedFiles(document)

                // Remove optional content (layers) that might hide redacted text
                scrubOptionalContent(document)
                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                    context = context,
                    bucket = DocForgeOutputBucket.DOCUMENTS
                )
                val sanitized = outputName.ifBlank { "redacted_${System.currentTimeMillis()}" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val outputFile = File(outputDir, "${sanitized}_irreversible.pdf")

                onProgress?.invoke(PdfRedactionProgress(stage = "Saving redacted PDF", current = 0, total = 1))
                document.save(outputFile)
                onProgress?.invoke(PdfRedactionProgress(stage = "Saving redacted PDF", current = 1, total = 1))

                if (options.verifyIrreversible) {
                    onProgress?.invoke(PdfRedactionProgress(stage = "Verifying irreversible redaction", current = 0, total = 1))
                    verifyTermsRemoved(outputFile = outputFile, terms = terms, caseSensitive = options.caseSensitive)
                    onProgress?.invoke(PdfRedactionProgress(stage = "Verifying irreversible redaction", current = 1, total = 1))
                }

                PdfRedactionResult(
                    outputFile = outputFile,
                    pageCount = pageCount,
                    removedTextOperatorCount = removedTextOps,
                    clearedFormFieldCount = clearedFormFieldCount,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    private fun redactPageTextOperators(
        document: PDDocument,
        page: PDPage,
        terms: List<String>,
        caseSensitive: Boolean
    ): Int {
        if (!page.hasContents()) return 0

        val parser = PDFStreamParser(page)
        parser.parse()
        val tokens = parser.tokens
        if (tokens.isEmpty()) return 0

        val rewritten = ArrayList<Any>(tokens.size)
        val pendingOperands = mutableListOf<COSBase>()
        var removedCount = 0

        tokens.forEach { token ->
            when (token) {
                is Operator -> {
                    val shouldRemove = shouldRedactTextOperator(
                        operator = token,
                        operands = pendingOperands,
                        terms = terms,
                        caseSensitive = caseSensitive
                    )
                    if (shouldRemove) {
                        removedCount += 1
                    } else {
                        rewritten.addAll(pendingOperands)
                        rewritten.add(token)
                    }
                    pendingOperands.clear()
                }

                is COSBase -> pendingOperands.add(token)
                else -> rewritten.add(token)
            }
        }

        if (pendingOperands.isNotEmpty()) {
            rewritten.addAll(pendingOperands)
        }

        if (removedCount <= 0) return 0

        val stream = PDStream(document)
        stream.createOutputStream(COSName.FLATE_DECODE).use { output ->
            ContentStreamWriter(output).writeTokens(rewritten)
        }
        page.setContents(stream)
        return removedCount
    }

    private fun shouldRedactTextOperator(
        operator: Operator,
        operands: List<COSBase>,
        terms: List<String>,
        caseSensitive: Boolean
    ): Boolean {
        val raw = extractShownText(operatorName = operator.name, operands = operands).trim()
        if (raw.isBlank()) return false
        return containsAnyTerm(
            text = raw,
            terms = terms,
            caseSensitive = caseSensitive
        )
    }

    private fun extractShownText(operatorName: String, operands: List<COSBase>): String {
        return when (operatorName) {
            OperatorName.SHOW_TEXT,
            OperatorName.SHOW_TEXT_LINE -> {
                (operands.lastOrNull() as? COSString)?.getString().orEmpty()
            }

            OperatorName.SHOW_TEXT_LINE_AND_SPACE -> {
                (operands.lastOrNull() as? COSString)?.getString().orEmpty()
            }

            OperatorName.SHOW_TEXT_ADJUSTED -> {
                val array = operands.lastOrNull() as? COSArray ?: return ""
                buildString {
                    for (index in 0 until array.size()) {
                        val part = array.getObject(index) as? COSString ?: continue
                        append(part.getString())
                    }
                }
            }

            else -> ""
        }
    }

    private fun clearMatchingFormValues(
        document: PDDocument,
        terms: List<String>,
        caseSensitive: Boolean
    ): Int {
        val form = document.documentCatalog?.acroForm ?: return 0
        val fields = form.fields.orEmpty()
        var cleared = 0

        fields.forEach { field ->
            val current = field.valueAsString.orEmpty()
            if (!containsAnyTerm(current, terms, caseSensitive)) return@forEach
            if (clearFieldValue(field)) {
                cleared += 1
            }
        }

        if (cleared > 0) {
            runCatching { form.refreshAppearances() }
        }
        return cleared
    }

    private fun clearFieldValue(field: PDField): Boolean {
        return runCatching {
            field.setValue("")
        }.isSuccess
    }

    private fun scrubMetadata(document: PDDocument) {
        document.setDocumentInformation(PDDocumentInformation())
        // Scrub XMP metadata stream entirely
        runCatching {
            document.documentCatalog?.metadata = null
        }
    }

    private fun scrubAnnotations(document: PDDocument, terms: List<String>, caseSensitive: Boolean) {
        for (i in 0 until document.numberOfPages) {
            val page = document.getPage(i)
            val annotations = page.annotations.orEmpty()
            val toRemove = annotations.filter { annot ->
                val contents = annot.contents.orEmpty()
                containsAnyTerm(contents, terms, caseSensitive)
            }
            if (toRemove.isNotEmpty()) {
                val remaining = annotations.toMutableList()
                remaining.removeAll(toRemove.toSet())
                page.annotations = remaining
            }
        }
    }

    private fun scrubEmbeddedFiles(document: PDDocument) {
        runCatching {
            document.documentCatalog?.names?.embeddedFiles = null
        }
    }

    private fun scrubOptionalContent(document: PDDocument) {
        runCatching {
            document.documentCatalog?.ocProperties = null
        }
    }

    private fun verifyTermsRemoved(
        outputFile: File,
        terms: List<String>,
        caseSensitive: Boolean
    ) {
        loadPdfDocument(outputFile).use { verificationDoc ->
            val text = PDFTextStripper().getText(verificationDoc)
            val remaining = findFirstRemainingTerm(
                text = text,
                terms = terms,
                caseSensitive = caseSensitive
            )
            if (remaining != null) {
                outputFile.delete()
                error(
                    "Redaction verification failed. Term '$remaining' is still discoverable in output text."
                )
            }
            // Also verify annotations are clean
            for (i in 0 until verificationDoc.numberOfPages) {
                for (annot in verificationDoc.getPage(i).annotations.orEmpty()) {
                    val annotRemaining = findFirstRemainingTerm(
                        text = annot.contents.orEmpty(),
                        terms = terms,
                        caseSensitive = caseSensitive
                    )
                    if (annotRemaining != null) {
                        outputFile.delete()
                        error(
                            "Redaction verification failed. Term '$annotRemaining' found in annotation."
                        )
                    }
                }
            }
        }
    }

    private fun containsAnyTerm(
        text: String,
        terms: List<String>,
        caseSensitive: Boolean
    ): Boolean {
        if (text.isBlank()) return false
        return findFirstRemainingTerm(text, terms, caseSensitive) != null
    }

    private fun findFirstRemainingTerm(
        text: String,
        terms: List<String>,
        caseSensitive: Boolean
    ): String? {
        val haystack = if (caseSensitive) {
            text
        } else {
            text.lowercase(Locale.getDefault())
        }
        return terms.firstOrNull { term ->
            val needle = if (caseSensitive) {
                term
            } else {
                term.lowercase(Locale.getDefault())
            }
            needle.isNotBlank() && haystack.contains(needle)
        }
    }

    private companion object {
        // PII detection regex patterns (offline, no ML).
        private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
        // North-American + international phone numbers (7-15 digits, optional country code, optional formatting).
        private val PHONE_REGEX = Regex("""\b(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b""")
        // US SSN with dashes; deliberately strict to avoid false positives.
        private val SSN_REGEX = Regex("""\b(?!000|666|9\d{2})\d{3}-(?!00)\d{2}-(?!0000)\d{4}\b""")
        // Credit card-like 13-19 digit sequences with optional spaces/dashes.
        private val CREDIT_CARD_REGEX = Regex("""\b(?:\d[ -]?){12,18}\d\b""")

        fun detectPiiTerms(text: String): List<String> {
            if (text.isBlank()) return emptyList()
            val matches = LinkedHashSet<String>()
            EMAIL_REGEX.findAll(text).forEach { matches.add(it.value) }
            PHONE_REGEX.findAll(text).forEach {
                val digits = it.value.filter { ch -> ch.isDigit() }
                if (digits.length in 7..15) matches.add(it.value)
            }
            SSN_REGEX.findAll(text).forEach { matches.add(it.value) }
            CREDIT_CARD_REGEX.findAll(text).forEach {
                val digits = it.value.filter { ch -> ch.isDigit() }
                if (digits.length in 13..19) matches.add(it.value)
            }
            return matches.toList()
        }
    }
}
