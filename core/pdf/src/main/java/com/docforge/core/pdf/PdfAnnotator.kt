package com.docforge.core.pdf

import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

enum class PdfAnnotationType {
    HIGHLIGHT,
    TEXT,
    STICKY_NOTE,
    FREEHAND
}

data class PdfFreehandPoint(
    val xRatio: Float,
    val yRatio: Float,
    val isBreak: Boolean = false
)

data class PdfAnnotationCommand(
    val pageOneBased: Int,
    val type: PdfAnnotationType,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float,
    val text: String = "",
    val colorArgb: Int = Color.YELLOW,
    val freehandPoints: List<PdfFreehandPoint> = emptyList()
)

class PdfAnnotator(
    private val context: Context
) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun annotate(
        inputUri: Uri,
        outputName: String,
        annotations: List<PdfAnnotationCommand>
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(annotations.isNotEmpty()) { "Add at least one annotation before export." }
        val checkCancelled = { coroutineContext.ensureActive() }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "annotated_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(outputDir, "$sanitized.pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_annotate_src_", suffix = ".pdf") { sourceFile ->
            PDDocument.load(sourceFile).use { sourceDoc ->
                require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

                annotations.forEach { command ->
                    require(command.pageOneBased in 1..sourceDoc.numberOfPages) {
                        "Annotation page ${command.pageOneBased} is out of bounds."
                    }
                }

                val commandsByPage = annotations.groupBy { it.pageOneBased }

                PDDocument().use { outDoc ->
                    repeat(sourceDoc.numberOfPages) { pageIndex ->
                        checkCancelled()
                        val sourcePage = sourceDoc.getPage(pageIndex)
                        val importedPage = importPage(outDoc, sourcePage)

                        val pageCommands = commandsByPage[pageIndex + 1].orEmpty()
                        if (pageCommands.isNotEmpty()) {
                            PDPageContentStream(
                                outDoc,
                                importedPage,
                                PDPageContentStream.AppendMode.APPEND,
                                true,
                                true
                            ).use { stream ->
                                pageCommands.forEach { command ->
                                    checkCancelled()
                                    drawAnnotation(stream, importedPage, command)
                                }
                            }
                        }
                    }

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
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_annotate_count_", suffix = ".pdf") { sourceFile ->
                PDDocument.load(sourceFile).use { sourceDoc -> sourceDoc.numberOfPages }
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

    private fun drawAnnotation(
        stream: PDPageContentStream,
        page: PDPage,
        command: PdfAnnotationCommand
    ) {
        when (command.type) {
            PdfAnnotationType.HIGHLIGHT -> drawHighlight(stream, page, command)
            PdfAnnotationType.TEXT -> drawTextBox(stream, page, command)
            PdfAnnotationType.STICKY_NOTE -> drawStickyNote(stream, page, command)
            PdfAnnotationType.FREEHAND -> drawFreehand(stream, page, command)
        }
    }

    private fun drawHighlight(
        stream: PDPageContentStream,
        page: PDPage,
        command: PdfAnnotationCommand
    ) {
        val rect = resolveRect(page, command)
        val color = blendWithWhite(command.colorArgb, 0.55f)

        stream.setNonStrokingColor(Color.red(color), Color.green(color), Color.blue(color))
        stream.addRect(rect.left, rect.bottom, rect.width, rect.height)
        stream.fill()
    }

    private fun drawTextBox(
        stream: PDPageContentStream,
        page: PDPage,
        command: PdfAnnotationCommand
    ) {
        val rect = resolveRect(page, command)
        val bgColor = blendWithWhite(command.colorArgb, 0.15f)

        stream.setNonStrokingColor(Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        stream.addRect(rect.left, rect.bottom, rect.width, rect.height)
        stream.fill()

        stream.setStrokingColor(50, 50, 50)
        stream.setLineWidth(1f)
        stream.addRect(rect.left, rect.bottom, rect.width, rect.height)
        stream.stroke()

        val font = PDType1Font.HELVETICA
        val fontSize = max(9f, min(14f, rect.height * 0.22f))
        val padding = 6f
        val maxTextWidth = (rect.width - padding * 2f).coerceAtLeast(20f)
        val maxLines = 4

        val lines = wrapText(
            text = command.text.ifBlank { "Note" },
            font = font,
            fontSize = fontSize,
            maxWidth = maxTextWidth,
            maxLines = maxLines
        )

        stream.setNonStrokingColor(0, 0, 0)
        var cursorY = rect.top - padding - fontSize
        lines.forEach { line ->
            if (cursorY < rect.bottom + padding) return@forEach
            stream.beginText()
            stream.setFont(font, fontSize)
            stream.newLineAtOffset(rect.left + padding, cursorY)
            stream.showText(sanitizePdfText(line))
            stream.endText()
            cursorY -= fontSize * 1.25f
        }
    }

    private fun drawStickyNote(
        stream: PDPageContentStream,
        page: PDPage,
        command: PdfAnnotationCommand
    ) {
        val rect = resolveRect(page, command)
        val noteSize = min(rect.width, rect.height).coerceIn(24f, 72f)

        stream.setNonStrokingColor(255, 235, 59)
        stream.addRect(rect.left, rect.top - noteSize, noteSize, noteSize)
        stream.fill()

        stream.setStrokingColor(80, 80, 80)
        stream.setLineWidth(1f)
        stream.addRect(rect.left, rect.top - noteSize, noteSize, noteSize)
        stream.stroke()

        if (command.text.isNotBlank()) {
            val font = PDType1Font.HELVETICA
            val fontSize = 10f
            val textLeft = rect.left + noteSize + 6f
            val textWidth = (rect.right - textLeft).coerceAtLeast(18f)
            val lines = wrapText(command.text, font, fontSize, textWidth, maxLines = 2)

            stream.setNonStrokingColor(0, 0, 0)
            var cursorY = rect.top - fontSize
            lines.forEach { line ->
                stream.beginText()
                stream.setFont(font, fontSize)
                stream.newLineAtOffset(textLeft, cursorY)
                stream.showText(sanitizePdfText(line))
                stream.endText()
                cursorY -= fontSize * 1.25f
            }
        }
    }

    private fun drawFreehand(
        stream: PDPageContentStream,
        page: PDPage,
        command: PdfAnnotationCommand
    ) {
        if (command.freehandPoints.isEmpty()) return

        val box = page.cropBox ?: page.mediaBox
        val width = box.width.coerceAtLeast(1f)
        val height = box.height.coerceAtLeast(1f)

        stream.setStrokingColor(
            Color.red(command.colorArgb),
            Color.green(command.colorArgb),
            Color.blue(command.colorArgb)
        )
        stream.setLineWidth(max(1.5f, width * 0.0025f))

        var started = false
        command.freehandPoints.forEach { point ->
            if (point.isBreak) {
                if (started) {
                    stream.stroke()
                }
                started = false
            } else {
                val x = box.lowerLeftX + (point.xRatio.coerceIn(0f, 1f) * width)
                val yFromTop = point.yRatio.coerceIn(0f, 1f) * height
                val y = box.lowerLeftY + (height - yFromTop)
                if (!started) {
                    stream.moveTo(x, y)
                    started = true
                } else {
                    stream.lineTo(x, y)
                }
            }
        }
        if (started) {
            stream.stroke()
        }
    }

    private fun resolveRect(page: PDPage, command: PdfAnnotationCommand): PdfRect {
        val box = page.cropBox ?: page.mediaBox
        val pageWidth = box.width.coerceAtLeast(1f)
        val pageHeight = box.height.coerceAtLeast(1f)

        val width = (pageWidth * command.widthRatio.coerceIn(0.05f, 0.95f)).coerceAtLeast(28f)
        val height = (pageHeight * command.heightRatio.coerceIn(0.03f, 0.9f)).coerceAtLeast(22f)

        val maxLeft = (pageWidth - width).coerceAtLeast(0f)
        val maxTopFromTop = (pageHeight - height).coerceAtLeast(0f)

        val left = box.lowerLeftX + (maxLeft * command.xRatio.coerceIn(0f, 1f))
        val topFromTop = maxTopFromTop * command.yRatio.coerceIn(0f, 1f)
        val bottom = box.lowerLeftY + (pageHeight - height - topFromTop).coerceAtLeast(0f)

        return PdfRect(
            left = left,
            bottom = bottom,
            width = width,
            height = height
        )
    }

    private fun wrapText(
        text: String,
        font: PDFont,
        fontSize: Float,
        maxWidth: Float,
        maxLines: Int
    ): List<String> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        var current = StringBuilder()

        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (measureTextWidth(candidate, font, fontSize) <= maxWidth) {
                current.clear()
                current.append(candidate)
            } else {
                if (current.isNotEmpty()) {
                    lines += current.toString()
                    if (lines.size >= maxLines) return lines
                    current.clear()
                }

                if (measureTextWidth(word, font, fontSize) <= maxWidth) {
                    current.append(word)
                } else {
                    var start = 0
                    while (start < word.length) {
                        var end = start + 1
                        while (end <= word.length) {
                            val chunk = word.substring(start, end)
                            if (measureTextWidth(chunk, font, fontSize) > maxWidth) break
                            end += 1
                        }
                        val safeEnd = (end - 1).coerceAtLeast(start + 1)
                        lines += word.substring(start, safeEnd)
                        if (lines.size >= maxLines) return lines
                        start = safeEnd
                    }
                }
            }
        }

        if (current.isNotEmpty() && lines.size < maxLines) {
            lines += current.toString()
        }
        return lines
    }

    private fun measureTextWidth(text: String, font: PDFont, fontSize: Float): Float {
        return runCatching {
            (font.getStringWidth(text) / 1000f) * fontSize
        }.getOrElse {
            text.length * fontSize * 0.55f
        }
    }

    private fun sanitizePdfText(text: String): String {
        return text
            .replace("\u0000", "")
            .replace('\n', ' ')
            .replace('\r', ' ')
    }

    private fun blendWithWhite(color: Int, whiteRatio: Float): Int {
        val ratio = whiteRatio.coerceIn(0f, 1f)
        val red = (Color.red(color) * (1f - ratio) + 255f * ratio).toInt().coerceIn(0, 255)
        val green = (Color.green(color) * (1f - ratio) + 255f * ratio).toInt().coerceIn(0, 255)
        val blue = (Color.blue(color) * (1f - ratio) + 255f * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(red, green, blue)
    }
}

private data class PdfRect(
    val left: Float,
    val bottom: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val top: Float get() = bottom + height
}
