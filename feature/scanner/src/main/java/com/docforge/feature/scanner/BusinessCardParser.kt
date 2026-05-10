package com.docforge.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.decodeBitmapConstrained
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Scans a business card image via ML Kit text recognition,
 * extracts contact information, and outputs a .vcf (vCard) file.
 *
 * Sprint 5 feature — Business Card Scanner → vCard.
 */
class BusinessCardParser(
    private val context: Context
) {

    data class ParsedContact(
        val fullName: String?,
        val jobTitle: String?,
        val company: String?,
        val emails: List<String>,
        val phones: List<String>,
        val websites: List<String>,
        val address: String?,
        val rawText: String
    )

    data class BusinessCardResult(
        val contact: ParsedContact,
        val vcfFile: File,
        val vcfSizeBytes: Long
    )

    /**
     * Scans image at [imageUri], extracts contact info, writes a .vcf file.
     */
    suspend fun scanAndExport(
        imageUri: Uri,
        outputName: String = ""
    ): BusinessCardResult = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmapConstrained(context, imageUri, maxLongEdge = 1200)
            ?: error("Failed to decode business card image.")

        val rawText = recognizeText(bitmap)
        bitmap.recycle()

        require(rawText.isNotBlank()) { "No text detected on the business card." }

        val contact = parseContact(rawText)
        val vcfContent = toVCard(contact)

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank {
            contact.fullName?.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                ?: "contact_${System.currentTimeMillis()}"
        }.replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val vcfFile = File(outputDir, "$sanitized.vcf")
        FileOutputStream(vcfFile).use { stream ->
            stream.write(vcfContent.toByteArray(Charsets.UTF_8))
        }

        BusinessCardResult(
            contact = contact,
            vcfFile = vcfFile,
            vcfSizeBytes = vcfFile.length()
        )
    }

    private suspend fun recognizeText(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    cont.resume(result.text)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    internal fun parseContact(rawText: String): ParsedContact {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        val emails = mutableListOf<String>()
        val phones = mutableListOf<String>()
        val websites = mutableListOf<String>()
        val otherLines = mutableListOf<String>()

        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val phoneRegex = Regex("\\+?[0-9][0-9\\s()./-]{6,}")
        val urlRegex = Regex("(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/\\S*)?")

        lines.forEach { line ->
            val foundEmails = emailRegex.findAll(line).map { it.value }.toList()
            val foundPhones = phoneRegex.findAll(line).map { it.value.trim() }.toList()
            val foundUrls = urlRegex.findAll(line).map { it.value }.filter { !it.contains("@") }.toList()

            emails += foundEmails
            phones += foundPhones
            websites += foundUrls

            if (foundEmails.isEmpty() && foundPhones.isEmpty() && foundUrls.isEmpty()) {
                otherLines += line
            }
        }

        // Heuristic: first line with mostly letters is the name, second is title/company
        val nameCandidates = otherLines.filter { it.matches(Regex("^[A-Za-z .'-]{2,50}$")) }
        val fullName = nameCandidates.firstOrNull()
        val jobTitle = if (nameCandidates.size > 1) nameCandidates[1] else null
        val company = otherLines.firstOrNull { it != fullName && it != jobTitle && it.length > 3 }

        // Address heuristic: line with numbers + common address words
        val address = otherLines.firstOrNull { line ->
            line.contains(Regex("\\d")) &&
                line.contains(Regex("(street|st|ave|road|rd|blvd|suite|floor|drive|dr|lane|ln|way|city|zip|\\d{5})", RegexOption.IGNORE_CASE))
        }

        return ParsedContact(
            fullName = fullName,
            jobTitle = jobTitle,
            company = company,
            emails = emails.distinct(),
            phones = phones.distinct(),
            websites = websites.distinct(),
            address = address,
            rawText = rawText
        )
    }

    private fun toVCard(contact: ParsedContact): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            contact.fullName?.let { appendLine("FN:$it") }
            contact.company?.let { appendLine("ORG:$it") }
            contact.jobTitle?.let { appendLine("TITLE:$it") }
            contact.emails.forEach { appendLine("EMAIL:$it") }
            contact.phones.forEach { appendLine("TEL:$it") }
            contact.websites.forEach { appendLine("URL:$it") }
            contact.address?.let { appendLine("ADR:;;$it;;;;") }
            appendLine("END:VCARD")
        }
    }
}
