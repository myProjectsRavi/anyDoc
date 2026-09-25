package com.docforge.core.pdf

import android.content.Context
import android.graphics.Color
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

data class PdfBatchStampOptions(
    val watermarkText: String,
    val watermarkEnabled: Boolean = true,
    val watermarkGray: Int = 180,
    val batesEnabled: Boolean = true,
    val batesPrefix: String = "EXHIBIT-",
    val batesStart: Int = 1,
    val batesPadding: Int = 4
)

data class PdfBatchStampFileResult(
    val inputUri: Uri,
    val outputFile: File,
    val pageCount: Int,
    val batesRangeStart: Int?,
    val batesRangeEnd: Int?,
    val outputSizeBytes: Long
)

data class PdfBatchStampResult(
    val outputs: List<PdfBatchStampFileResult>,
    val totalPages: Int,
    val totalOutputSizeBytes: Long
)

class PdfBatchStampTool(
    private val context: Context
) {
    suspend fun stampBatch(
        inputUris: List<Uri>,
        outputBaseName: String,
        options: PdfBatchStampOptions
    ): PdfBatchStampResult = withContext(Dispatchers.IO) {
        require(inputUris.isNotEmpty()) { "Select at least one PDF." }
        require(options.watermarkEnabled || options.batesEnabled) {
            "Enable watermark, Bates stamping, or both."
        }

        val checkCancelled = { coroutineContext.ensureActive() }
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val outputs = mutableListOf<PdfBatchStampFileResult>()
        var batesCounter = options.batesStart.coerceAtLeast(1)

        inputUris.forEachIndexed { inputIndex, inputUri ->
            checkCancelled()
            context.withUriCopiedToCacheFile(
                inputUri,
                prefix = "docforge_batch_stamp_src_",
                suffix = ".pdf"
            ) { sourceFile ->
                loadPdfDocument(sourceFile).use { sourceDoc ->
                    require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages: $inputUri" }

                    PDDocument().use { outDoc ->
                        var batesStartForFile: Int? = null
                        var batesEndForFile: Int? = null

                        repeat(sourceDoc.numberOfPages) { pageIndex ->
                            checkCancelled()
                            val sourcePage = sourceDoc.getPage(pageIndex)
                            val importedPage = importPage(outDoc, sourcePage)

                            PDPageContentStream(
                                outDoc,
                                importedPage,
                                PDPageContentStream.AppendMode.APPEND,
                                true,
                                true
                            ).use { stream ->
                                if (options.watermarkEnabled && options.watermarkText.isNotBlank()) {
                                    drawWatermark(stream, importedPage, options)
                                }

                                if (options.batesEnabled) {
                                    val current = batesCounter++
                                    if (batesStartForFile == null) batesStartForFile = current
                                    batesEndForFile = current
                                    drawBatesLabel(stream, importedPage, options, current)
                                }
                            }
                        }

                        val base = outputBaseName.ifBlank { "batch_stamped_${System.currentTimeMillis()}" }
                            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        val outputFile = withStagedOutputFile(
                            directory = outputDir,
                            baseName = "${base}_${inputIndex + 1}",
                            extension = "pdf"
                        ) { stagedFile ->
                            outDoc.save(stagedFile)
                        }.outputFile

                        outputs += PdfBatchStampFileResult(
                            inputUri = inputUri,
                            outputFile = outputFile,
                            pageCount = sourceDoc.numberOfPages,
                            batesRangeStart = batesStartForFile,
                            batesRangeEnd = batesEndForFile,
                            outputSizeBytes = outputFile.length()
                        )
                    }
                }
            }
        }

        PdfBatchStampResult(
            outputs = outputs,
            totalPages = outputs.sumOf { it.pageCount },
            totalOutputSizeBytes = outputs.sumOf { it.outputSizeBytes }
        )
    }

    private fun drawWatermark(
        stream: PDPageContentStream,
        page: PDPage,
        options: PdfBatchStampOptions
    ) {
        val box = page.cropBox ?: page.mediaBox
        val pageWidth = box.width.coerceAtLeast(1f)
        val pageHeight = box.height.coerceAtLeast(1f)
        val centerX = box.lowerLeftX + (pageWidth / 2f)
        val centerY = box.lowerLeftY + (pageHeight / 2f)
        val diagonal = hypot(pageWidth.toDouble(), pageHeight.toDouble()).toFloat().coerceAtLeast(1f)
        val fontSize = (diagonal * 0.065f).coerceIn(28f, 72f)

        val angle = atan2(pageHeight.toDouble(), pageWidth.toDouble()).toFloat()
        val text = options.watermarkText.trim().ifBlank { "CONFIDENTIAL" }
        val textWidth = (PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000f) * fontSize

        val gray = options.watermarkGray.coerceIn(90, 230)

        stream.saveGraphicsState()
        stream.transform(
            Matrix.getRotateInstance(
                angle.toDouble(),
                centerX,
                centerY
            )
        )
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, fontSize)
        stream.setNonStrokingColor(gray, gray, gray)
        stream.newLineAtOffset(centerX - (textWidth / 2f), centerY)
        stream.showText(sanitizePdfText(text))
        stream.endText()
        stream.restoreGraphicsState()
    }

    private fun drawBatesLabel(
        stream: PDPageContentStream,
        page: PDPage,
        options: PdfBatchStampOptions,
        sequence: Int
    ) {
        val box = page.cropBox ?: page.mediaBox
        val pageWidth = box.width.coerceAtLeast(1f)
        val pageHeight = box.height.coerceAtLeast(1f)

        val fontSize = max(9f, pageHeight * 0.016f)
        val label = buildBatesLabel(options, sequence)
        val textWidth = (PDType1Font.HELVETICA.getStringWidth(label) / 1000f) * fontSize

        val margin = 22f
        val x = box.lowerLeftX + (pageWidth - margin - textWidth).coerceAtLeast(8f)
        val y = box.lowerLeftY + margin

        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA, fontSize)
        stream.setNonStrokingColor(Color.DKGRAY)
        stream.newLineAtOffset(x, y)
        stream.showText(sanitizePdfText(label))
        stream.endText()
    }

    private fun buildBatesLabel(options: PdfBatchStampOptions, sequence: Int): String {
        val pad = options.batesPadding.coerceIn(1, 10)
        val numeric = String.format(Locale.US, "%0${pad}d", sequence.coerceAtLeast(0))
        return "${options.batesPrefix}$numeric"
    }

    private fun importPage(outDoc: PDDocument, sourcePage: PDPage): PDPage {
        val imported = outDoc.importPage(sourcePage)
        imported.rotation = sourcePage.rotation
        imported.mediaBox = sourcePage.mediaBox
        imported.cropBox = sourcePage.cropBox
        imported.resources = sourcePage.resources
        return imported
    }

    private fun sanitizePdfText(raw: String): String {
        return raw.replace('\u0000', ' ').replace(Regex("\\s+"), " ").trim()
    }
}
