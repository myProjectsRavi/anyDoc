package com.docforge.feature.converter

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.PdfCreationResult
import com.docforge.core.pdf.PdfPageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class TextPdfFormatMode {
    PLAIN,
    SMART
}

data class TextPdfConversionOptions(
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val title: String = "",
    val formatMode: TextPdfFormatMode = TextPdfFormatMode.SMART
)

data class TextPdfConversionResult(
    val pdfResult: PdfCreationResult,
    val paragraphCount: Int,
    val characterCount: Int
)

class TextPdfConverter(
    private val context: Context
) {

    suspend fun convert(
        rawText: String,
        outputName: String,
        options: TextPdfConversionOptions
    ): TextPdfConversionResult = withContext(Dispatchers.IO) {
        val normalizedText = rawText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        require(normalizedText.trim().isNotEmpty()) { "Enter some text first." }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "text_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(outputDir, "$sanitized.pdf")

        val blocks = buildBlocks(
            title = options.title.trim(),
            text = normalizedText,
            formatMode = options.formatMode
        )
        val pageCount = writeBlocksAsPdf(
            blocks = blocks,
            pageSize = options.pageSize,
            outputFile = outputFile
        )

        TextPdfConversionResult(
            pdfResult = PdfCreationResult(
                outputFile = outputFile,
                pageCount = pageCount,
                outputSizeBytes = outputFile.length()
            ),
            paragraphCount = normalizedText.lineSequence().count { it.isNotBlank() },
            characterCount = normalizedText.length
        )
    }

    private fun buildBlocks(
        title: String,
        text: String,
        formatMode: TextPdfFormatMode
    ): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()
        if (title.isNotBlank()) {
            blocks += TextBlock.Title(title)
        }

        val lines = text.split('\n')
        if (formatMode == TextPdfFormatMode.PLAIN) {
            lines.forEach { line ->
                if (line.isBlank()) {
                    blocks += TextBlock.Spacer
                } else {
                    blocks += TextBlock.Paragraph(line.trimEnd())
                }
            }
            return blocks
        }

        val numberedRegex = Regex("^(\\d+)[.)]\\s+(.+)$")

        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            when {
                trimmed.isBlank() -> blocks += TextBlock.Spacer
                trimmed.startsWith("### ") -> blocks += TextBlock.Heading(
                    text = trimmed.removePrefix("### ").trim(),
                    level = 3
                )

                trimmed.startsWith("## ") -> blocks += TextBlock.Heading(
                    text = trimmed.removePrefix("## ").trim(),
                    level = 2
                )

                trimmed.startsWith("# ") -> blocks += TextBlock.Heading(
                    text = trimmed.removePrefix("# ").trim(),
                    level = 1
                )

                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    blocks += TextBlock.Bullet(trimmed.drop(2).trim())
                }

                numberedRegex.matches(trimmed) -> {
                    val match = numberedRegex.find(trimmed)
                    val numberPrefix = match?.groupValues?.get(1).orEmpty() + ". "
                    val numberedText = match?.groupValues?.get(2).orEmpty()
                    blocks += TextBlock.Numbered(numberPrefix = numberPrefix, text = numberedText)
                }

                else -> blocks += TextBlock.Paragraph(line)
            }
        }
        return blocks
    }

    private fun writeBlocksAsPdf(
        blocks: List<TextBlock>,
        pageSize: PdfPageSize,
        outputFile: File
    ): Int {
        val (pageWidth, pageHeight) = resolvePageSize(pageSize)
        val margin = 42f
        val maxTextWidth = pageWidth - (margin * 2)

        val bodyPaint = basePaint(textSize = 12f, bold = false)
        val heading1Paint = basePaint(textSize = 18f, bold = true)
        val heading2Paint = basePaint(textSize = 16f, bold = true)
        val heading3Paint = basePaint(textSize = 14f, bold = true)
        val titlePaint = basePaint(textSize = 22f, bold = true)

        val pdf = PdfDocument()
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        var y = margin

        fun startNewPageIfNeeded(minHeight: Float) {
            if (y + minHeight <= pageHeight - margin) return
            pdf.finishPage(page)
            pageNumber += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            y = margin
        }

        blocks.forEach { block ->
            when (block) {
                is TextBlock.Spacer -> {
                    val gap = bodyPaint.textSize * 0.9f
                    startNewPageIfNeeded(gap)
                    y += gap
                }

                is TextBlock.Title -> {
                    val lines = wrapLine(block.text, titlePaint, maxTextWidth)
                    val lineHeight = titlePaint.textSize * 1.35f
                    startNewPageIfNeeded(lines.size * lineHeight + lineHeight)
                    lines.forEach { line ->
                        val width = titlePaint.measureText(line)
                        val x = ((pageWidth - width) / 2f).coerceAtLeast(margin)
                        canvas.drawText(line, x, y + titlePaint.textSize, titlePaint)
                        y += lineHeight
                    }
                    y += titlePaint.textSize * 0.4f
                }

                is TextBlock.Heading -> {
                    val paint = when (block.level) {
                        1 -> heading1Paint
                        2 -> heading2Paint
                        else -> heading3Paint
                    }
                    val lines = wrapLine(block.text, paint, maxTextWidth)
                    val lineHeight = paint.textSize * 1.35f
                    startNewPageIfNeeded(lines.size * lineHeight + lineHeight)
                    lines.forEach { line ->
                        canvas.drawText(line, margin, y + paint.textSize, paint)
                        y += lineHeight
                    }
                    y += paint.textSize * 0.2f
                }

                is TextBlock.Paragraph -> {
                    val lines = wrapLine(block.text, bodyPaint, maxTextWidth)
                    val lineHeight = bodyPaint.textSize * 1.45f
                    startNewPageIfNeeded(lines.size * lineHeight)
                    lines.forEach { line ->
                        canvas.drawText(line, margin, y + bodyPaint.textSize, bodyPaint)
                        y += lineHeight
                    }
                }

                is TextBlock.Bullet -> {
                    val bulletText = "- ${block.text}"
                    val lines = wrapLine(bulletText, bodyPaint, maxTextWidth)
                    val lineHeight = bodyPaint.textSize * 1.45f
                    startNewPageIfNeeded(lines.size * lineHeight)
                    lines.forEach { line ->
                        canvas.drawText(line, margin, y + bodyPaint.textSize, bodyPaint)
                        y += lineHeight
                    }
                }

                is TextBlock.Numbered -> {
                    val numberedLine = block.numberPrefix + block.text
                    val lines = wrapLine(numberedLine, bodyPaint, maxTextWidth)
                    val lineHeight = bodyPaint.textSize * 1.45f
                    startNewPageIfNeeded(lines.size * lineHeight)
                    lines.forEach { line ->
                        canvas.drawText(line, margin, y + bodyPaint.textSize, bodyPaint)
                        y += lineHeight
                    }
                }
            }
        }

        if (blocks.isEmpty()) {
            canvas.drawText("[No text provided]", margin, y + bodyPaint.textSize, bodyPaint)
        }

        pdf.finishPage(page)
        FileOutputStream(outputFile).use { stream ->
            pdf.writeTo(stream)
        }
        pdf.close()
        return pageNumber
    }

    private fun resolvePageSize(pageSize: PdfPageSize): Pair<Int, Int> {
        return when (pageSize) {
            PdfPageSize.A4 -> 595 to 842
            PdfPageSize.LETTER -> 612 to 792
            PdfPageSize.LEGAL -> 612 to 1008
            PdfPageSize.AUTO -> 595 to 842
        }
    }

    private fun wrapLine(line: String, paint: Paint, maxWidth: Float): List<String> {
        if (line.isBlank()) return listOf(" ")
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

    private fun basePaint(textSize: Float, bold: Boolean): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }
    }
}

private sealed interface TextBlock {
    data object Spacer : TextBlock
    data class Title(val text: String) : TextBlock
    data class Heading(val text: String, val level: Int) : TextBlock
    data class Paragraph(val text: String) : TextBlock
    data class Bullet(val text: String) : TextBlock
    data class Numbered(val numberPrefix: String, val text: String) : TextBlock
}
