package com.docforge.feature.converter

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Xml
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.PdfCreationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

enum class DocumentInputType {
    DOCX,
    RTF,
    CSV,
    TXT,
    UNKNOWN
}

data class DocumentPdfConversionResult(
    val pdfResult: PdfCreationResult,
    val inputType: DocumentInputType,
    val lineCount: Int
)

class DocumentPdfConverter(
    private val context: Context
) {

    suspend fun convertToPdf(
        inputUri: Uri,
        inputNameHint: String?,
        outputName: String
    ): DocumentPdfConversionResult = withContext(Dispatchers.IO) {
        val inputType = detectInputType(inputUri, inputNameHint)
        val lines = extractLines(inputUri, inputType)
        require(lines.isNotEmpty()) { "Input document has no extractable content." }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "doc_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(outputDir, "$sanitized.pdf")

        val pageCount = writeLinesAsPdf(lines, outputFile)

        DocumentPdfConversionResult(
            pdfResult = PdfCreationResult(
                outputFile = outputFile,
                pageCount = pageCount,
                outputSizeBytes = outputFile.length()
            ),
            inputType = inputType,
            lineCount = lines.size
        )
    }

    fun detectInputType(inputNameHint: String?): DocumentInputType {
        val extension = inputNameHint
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.getDefault())
            .orEmpty()

        return when (extension) {
            "docx" -> DocumentInputType.DOCX
            "rtf" -> DocumentInputType.RTF
            "csv" -> DocumentInputType.CSV
            "txt" -> DocumentInputType.TXT
            else -> DocumentInputType.UNKNOWN
        }
    }

    fun detectInputType(inputUri: Uri, inputNameHint: String?): DocumentInputType {
        val fromExtension = detectInputType(inputNameHint)
        if (fromExtension != DocumentInputType.UNKNOWN) {
            return fromExtension
        }
        return detectInputTypeByContent(inputUri)
    }

    private fun extractLines(inputUri: Uri, inputType: DocumentInputType): List<String> {
        return when (inputType) {
            DocumentInputType.DOCX -> extractDocxLines(inputUri)
            DocumentInputType.RTF -> extractRtfLines(inputUri)
            DocumentInputType.CSV -> extractCsvLines(inputUri)
            DocumentInputType.TXT,
            DocumentInputType.UNKNOWN -> extractTextLines(inputUri)
        }.ifEmpty { listOf("[No extractable text found]") }
    }

    private fun detectInputTypeByContent(inputUri: Uri): DocumentInputType {
        val header = context.contentResolver.openInputStream(inputUri)?.use { input ->
            val bytes = ByteArray(512)
            val read = input.read(bytes)
            if (read <= 0) {
                ""
            } else {
                String(bytes, 0, read, Charsets.ISO_8859_1)
            }
        }.orEmpty()

        if (header.startsWith("{\\rtf")) {
            return DocumentInputType.RTF
        }
        if (header.startsWith("PK") && looksLikeDocx(inputUri)) {
            return DocumentInputType.DOCX
        }

        val normalized = header.replace("\r\n", "\n").replace('\r', '\n')
        val firstLines = normalized.lineSequence().take(5).toList()
        val likelyCsv = firstLines.any { line ->
            val commaCount = line.count { it == ',' }
            commaCount >= 2 && line.any { ch -> ch.isLetterOrDigit() }
        }
        if (likelyCsv) {
            return DocumentInputType.CSV
        }

        return DocumentInputType.TXT
    }

    private fun looksLikeDocx(inputUri: Uri): Boolean {
        return context.contentResolver.openInputStream(inputUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }
                    .any { entry -> entry.name == "word/document.xml" }
            }
        } ?: false
    }

    private fun extractDocxLines(inputUri: Uri): List<String> {
        return context.contentResolver.openInputStream(inputUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var documentXml: List<String>? = null
                val imageEntries = mutableListOf<String>()
                generateSequence { zip.nextEntry }.forEach { entry ->
                    when {
                        entry.name == "word/document.xml" -> {
                            documentXml = parseDocxDocumentXml(zip)
                        }
                        entry.name.startsWith("word/media/") -> {
                            imageEntries += entry.name
                        }
                    }
                }
                val lines = documentXml ?: error("Unable to read DOCX file.")
                // Append image references for user awareness
                if (imageEntries.isNotEmpty()) {
                    lines + listOf("", "[${imageEntries.size} embedded image(s) detected — visual rendering not yet supported]")
                } else {
                    lines
                }
            }
        } ?: error("Unable to read DOCX file.")
    }

    private fun extractRtfLines(inputUri: Uri): List<String> {
        val raw = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { reader ->
            reader.readText()
        } ?: error("Unable to read RTF file.")

        var plain = raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\\\\par[d]?\\s?"), "\n")
            .replace(Regex("\\\\tab\\s?"), "\t")

        plain = Regex("\\\\'[0-9A-Fa-f]{2}").replace(plain) { match ->
            val hex = match.value.substring(2)
            hex.toInt(16).toChar().toString()
        }

        plain = plain
            .replace(Regex("\\\\[a-zA-Z]+-?\\d*\\s?"), "")
            .replace("{", "")
            .replace("}", "")
            .replace("\\\\", "\\")

        return plain
            .split('\n')
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
    }

    private fun extractCsvLines(inputUri: Uri): List<String> {
        val rows = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { reader ->
            reader.readLines()
        } ?: error("Unable to read CSV file.")

        return rows.map { row ->
            parseCsvRow(row).joinToString(separator = " | ") { cell -> cell.trim() }
        }
    }

    private fun parseCsvRow(row: String): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val ch = row[i]
            when {
                ch == '"' && inQuotes && i + 1 < row.length && row[i + 1] == '"' -> {
                    sb.append('"')
                    i++
                }

                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    cells += sb.toString()
                    sb.clear()
                }

                else -> sb.append(ch)
            }
            i++
        }
        cells += sb.toString()
        return cells
    }

    private fun extractTextLines(inputUri: Uri): List<String> {
        val text = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { reader ->
            reader.readText()
        } ?: error("Unable to read text file.")

        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split('\n')
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
    }

    private fun writeLinesAsPdf(lines: List<String>, outputFile: File): Int {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
        }
        val lineHeight = paint.textSize * 1.5f
        val maxTextWidth = pageWidth - (margin * 2)

        val pdf = PdfDocument()
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + paint.textSize

        val wrappedLines = lines.flatMap { wrapLine(it, paint, maxTextWidth) }

        wrappedLines.forEach { line ->
            if (y > pageHeight - margin) {
                pdf.finishPage(page)
                pageNumber += 1
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin + paint.textSize
            }
            canvas.drawText(line, margin, y, paint)
            y += lineHeight
        }

        if (wrappedLines.isEmpty()) {
            canvas.drawText("[No extractable text found]", margin, y, paint)
        }

        pdf.finishPage(page)

        FileOutputStream(outputFile).use { stream ->
            pdf.writeTo(stream)
        }
        pdf.close()
        return pageNumber
    }

    private fun wrapLine(line: String, paint: Paint, maxWidth: Float): List<String> {
        if (line.isEmpty()) return listOf(" ")

        val result = mutableListOf<String>()
        var start = 0
        while (start < line.length) {
            var count = paint.breakText(line, start, line.length, true, maxWidth, null)
            if (count <= 0) break
            var end = (start + count).coerceAtMost(line.length)

            if (end < line.length) {
                val candidate = line.substring(start, end)
                val lastSpace = candidate.lastIndexOf(' ')
                if (lastSpace > 0) {
                    end = start + lastSpace + 1
                }
            }

            val chunk = line.substring(start, end).trimEnd()
            result += if (chunk.isEmpty()) " " else chunk
            start = end
        }

        return result.ifEmpty { listOf(" ") }
    }

    private fun parseDocxDocumentXml(xmlEntryStream: java.io.InputStream): List<String> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(xmlEntryStream, "UTF-8")
        }

        val lines = mutableListOf<String>()
        val paragraph = StringBuilder()
        var insideTextNode = false
        var isBold = false
        var isItalic = false
        var insideTable = false
        val tableRow = mutableListOf<String>()
        val tableCell = StringBuilder()
        var insideTableCell = false
        var isListItem = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        // Table elements
                        "tbl" -> insideTable = true
                        "tr" -> tableRow.clear()
                        "tc" -> {
                            insideTableCell = true
                            tableCell.clear()
                        }
                        // Paragraph
                        "p" -> {
                            paragraph.clear()
                            isBold = false
                            isItalic = false
                            isListItem = false
                        }
                        // Run properties
                        "b" -> isBold = true
                        "i" -> isItalic = true
                        // List numbering
                        "numPr" -> isListItem = true
                        // Text run
                        "t" -> insideTextNode = true
                        // Whitespace
                        "tab" -> paragraph.append('\t')
                        "br", "cr" -> paragraph.append('\n')
                    }
                }

                XmlPullParser.TEXT -> {
                    if (insideTextNode) {
                        val text = parser.text
                        // Apply inline formatting markers for PDF rendering
                        val formatted = buildString {
                            if (isBold && isItalic) append("***")
                            else if (isBold) append("**")
                            else if (isItalic) append("*")
                            append(text)
                            if (isBold && isItalic) append("***")
                            else if (isBold) append("**")
                            else if (isItalic) append("*")
                        }
                        if (insideTableCell) {
                            tableCell.append(text)
                        } else {
                            paragraph.append(formatted)
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> insideTextNode = false
                        "rPr" -> {
                            // Reset run properties at end of run props
                        }
                        "r" -> {
                            isBold = false
                            isItalic = false
                        }
                        "p" -> {
                            if (insideTableCell) {
                                // Accumulate cell text
                                if (tableCell.isNotEmpty()) tableCell.append(" ")
                            } else {
                                val content = paragraph.toString().trimEnd()
                                if (content.isNotBlank()) {
                                    val prefix = if (isListItem) "  • " else ""
                                    lines += "$prefix$content"
                                }
                                paragraph.clear()
                            }
                        }
                        "tc" -> {
                            insideTableCell = false
                            tableRow += tableCell.toString().trim()
                            tableCell.clear()
                        }
                        "tr" -> {
                            if (tableRow.isNotEmpty()) {
                                lines += tableRow.joinToString(" | ") { it.ifBlank { " " } }
                            }
                            tableRow.clear()
                        }
                        "tbl" -> {
                            insideTable = false
                            lines += "" // blank line after table
                        }
                    }
                }
            }
            event = parser.next()
        }

        return lines
    }
}
