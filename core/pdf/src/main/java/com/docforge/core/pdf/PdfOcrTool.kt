package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

data class PdfOcrResult(
    val textOutputFile: File,
    val searchablePdfFile: File?,
    val pageCount: Int,
    val lineCount: Int,
    val extractedText: String,
    val extractedChars: Int,
    val outputSizeBytes: Long
)

data class PdfOcrProgress(
    val stage: String,
    val current: Int,
    val total: Int
)

private data class OcrLine(
    val pageOneBased: Int,
    val text: String,
    val bbox: Rect,
    val bitmapWidth: Int,
    val bitmapHeight: Int
)

class PdfOcrTool(
    private val context: Context
) {
    suspend fun process(
        inputUri: Uri,
        outputName: String,
        createSearchablePdf: Boolean,
        pageProgressChunk: Int = 2,
        onProgress: ((PdfOcrProgress) -> Unit)? = null
    ): PdfOcrResult = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(inputUri).orEmpty().lowercase(Locale.getDefault())
        val isPdfLike = mime.contains("pdf") || inputUri.toString().lowercase(Locale.getDefault()).endsWith(".pdf")

        return@withContext if (isPdfLike) {
            processPdf(
                inputUri = inputUri,
                outputName = outputName,
                createSearchablePdf = createSearchablePdf,
                pageProgressChunk = pageProgressChunk,
                onProgress = onProgress
            )
        } else {
            processImage(
                inputUri = inputUri,
                outputName = outputName,
                createSearchablePdf = createSearchablePdf,
                onProgress = onProgress
            )
        }
    }

    private suspend fun processPdf(
        inputUri: Uri,
        outputName: String,
        createSearchablePdf: Boolean,
        pageProgressChunk: Int,
        onProgress: ((PdfOcrProgress) -> Unit)?
    ): PdfOcrResult {
        val coroutineCtx = currentCoroutineContext()
        val progressChunk = pageProgressChunk.coerceAtLeast(1)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            return context.withUriCopiedToCacheFile(
                inputUri,
                prefix = "docforge_ocr_src_",
                suffix = ".pdf"
            ) { sourceFile ->
                val allLines = mutableListOf<OcrLine>()
                var pageCount: Int

                ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        pageCount = renderer.pageCount
                        require(pageCount > 0) { "Input PDF has no pages." }
                        onProgress?.invoke(PdfOcrProgress(stage = "Detecting text", current = 0, total = pageCount))

                        repeat(renderer.pageCount) { pageIndex ->
                            coroutineCtx.ensureActive()
                            renderer.openPage(pageIndex).use { page ->
                                val targetLongEdge = 1800
                                val sourceWidth = page.width.coerceAtLeast(1)
                                val sourceHeight = page.height.coerceAtLeast(1)
                                val largestSide = max(sourceWidth, sourceHeight).coerceAtLeast(1)
                                val scale = (targetLongEdge.toFloat() / largestSide.toFloat()).coerceAtMost(1f)
                                val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
                                val height = (sourceHeight * scale).toInt().coerceAtLeast(1)

                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                try {
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
                                    allLines += extractLines(
                                        pageOneBased = pageIndex + 1,
                                        bitmap = bitmap,
                                        text = text
                                    )
                                    val processed = pageIndex + 1
                                    if (processed == pageCount || processed % progressChunk == 0) {
                                        onProgress?.invoke(
                                            PdfOcrProgress(
                                                stage = "Detecting text",
                                                current = processed,
                                                total = pageCount
                                            )
                                        )
                                    }
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }
                    }
                }

                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                    context = context,
                    bucket = DocForgeOutputBucket.DOCUMENTS
                )
                val base = outputName.ifBlank { "ocr_${System.currentTimeMillis()}" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")

                val textOutput = resolveNonConflictingFile(outputDir, "${base}_ocr", "txt")
                val extracted = buildOcrText(pageCount, allLines)
                textOutput.writeText(extracted)

                val searchablePdf = if (createSearchablePdf) {
                    val pdfOutput = resolveNonConflictingFile(outputDir, "${base}_searchable", "pdf")
                    writeSearchablePdf(
                        sourceFile = sourceFile,
                        outputFile = pdfOutput,
                        lines = allLines,
                        pageProgressChunk = progressChunk,
                        onProgress = onProgress
                    )
                    pdfOutput
                } else {
                    null
                }

                PdfOcrResult(
                    textOutputFile = textOutput,
                    searchablePdfFile = searchablePdf,
                    pageCount = pageCount,
                    lineCount = allLines.size,
                    extractedText = extracted,
                    extractedChars = extracted.length,
                    outputSizeBytes = textOutput.length() + (searchablePdf?.length() ?: 0L)
                )
            }
        } finally {
            recognizer.close()
        }
    }

    private suspend fun processImage(
        inputUri: Uri,
        outputName: String,
        createSearchablePdf: Boolean,
        onProgress: ((PdfOcrProgress) -> Unit)?
    ): PdfOcrResult {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val bitmap = decodeBitmapConstrained(context, inputUri, maxLongEdge = 2200)
                ?: error("Unable to decode selected image.")
            try {
                onProgress?.invoke(PdfOcrProgress(stage = "Detecting text", current = 0, total = 1))
                val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
                val ocrLines = extractLines(pageOneBased = 1, bitmap = bitmap, text = text)
                val lines = ocrLines.map { it.text }
                    .filter { it.isNotBlank() }
                val extracted = if (lines.isEmpty()) text.text.trim() else lines.joinToString("\n")
                onProgress?.invoke(PdfOcrProgress(stage = "Detecting text", current = 1, total = 1))

                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                    context = context,
                    bucket = DocForgeOutputBucket.DOCUMENTS
                )
                val base = outputName.ifBlank { "image_ocr_${System.currentTimeMillis()}" }
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val textOutput = resolveNonConflictingFile(outputDir, base, "txt")
                textOutput.writeText(extracted)

                val searchablePdf = if (createSearchablePdf) {
                    val pdfOutput = File(outputDir, "${base}_searchable.pdf")
                    onProgress?.invoke(PdfOcrProgress(stage = "Embedding OCR text layer", current = 0, total = 1))
                    writeSearchablePdfFromImage(bitmap = bitmap, outputFile = pdfOutput, lines = ocrLines)
                    onProgress?.invoke(PdfOcrProgress(stage = "Embedding OCR text layer", current = 1, total = 1))
                    pdfOutput
                } else {
                    null
                }

                return PdfOcrResult(
                    textOutputFile = textOutput,
                    searchablePdfFile = searchablePdf,
                    pageCount = 1,
                    lineCount = lines.size,
                    extractedText = extracted,
                    extractedChars = extracted.length,
                    outputSizeBytes = textOutput.length() + (searchablePdf?.length() ?: 0L)
                )
            } finally {
                bitmap.recycle()
            }
        } finally {
            recognizer.close()
        }
    }

    private fun extractLines(pageOneBased: Int, bitmap: Bitmap, text: Text): List<OcrLine> {
        val lines = mutableListOf<OcrLine>()
        text.textBlocks.forEach { block ->
            block.lines.forEach lineLoop@{ line ->
                val raw = line.text.trim()
                val bbox = line.boundingBox
                if (raw.isBlank() || bbox == null || bbox.width() <= 0 || bbox.height() <= 0) {
                    return@lineLoop
                }
                lines += OcrLine(
                    pageOneBased = pageOneBased,
                    text = raw,
                    bbox = Rect(bbox),
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                )
            }
        }
        return lines
    }

    private fun buildOcrText(pageCount: Int, lines: List<OcrLine>): String {
        val grouped = lines.groupBy { it.pageOneBased }
        val builder = StringBuilder()
        repeat(pageCount) { pageIndex ->
            val page = pageIndex + 1
            builder.append("=== Page ").append(page).append(" ===\n")
            grouped[page].orEmpty().forEach { line ->
                builder.append(line.text).append('\n')
            }
            builder.append('\n')
        }
        return builder.toString().trim()
    }

    private suspend fun writeSearchablePdf(
        sourceFile: File,
        outputFile: File,
        lines: List<OcrLine>,
        pageProgressChunk: Int,
        onProgress: ((PdfOcrProgress) -> Unit)?
    ) {
        val coroutineCtx = currentCoroutineContext()
        loadPdfDocument(sourceFile).use { sourceDoc ->
            PDDocument().use { outDoc ->
                val linesByPage = lines.groupBy { it.pageOneBased }
                val pageCount = sourceDoc.numberOfPages
                val progressChunk = pageProgressChunk.coerceAtLeast(1)
                onProgress?.invoke(PdfOcrProgress(stage = "Embedding OCR text layer", current = 0, total = pageCount))

                repeat(sourceDoc.numberOfPages) { pageIndex ->
                    coroutineCtx.ensureActive()
                    val sourcePage = sourceDoc.getPage(pageIndex)
                    val imported = importPage(outDoc, sourcePage)

                    val pageLines = linesByPage[pageIndex + 1].orEmpty()
                    if (pageLines.isEmpty()) return@repeat

                    val box = imported.cropBox ?: imported.mediaBox
                    val pageWidth = box.width.coerceAtLeast(1f)
                    val pageHeight = box.height.coerceAtLeast(1f)

                    PDPageContentStream(
                        outDoc,
                        imported,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    ).use { stream ->
                        stream.setRenderingMode(RenderingMode.NEITHER)
                        stream.setFont(PDType1Font.HELVETICA, 10f)

                        pageLines.forEach { line ->
                            val scaleX = pageWidth / line.bitmapWidth.toFloat().coerceAtLeast(1f)
                            val scaleY = pageHeight / line.bitmapHeight.toFloat().coerceAtLeast(1f)
                            val pdfLeft = box.lowerLeftX + (line.bbox.left * scaleX)
                            val pdfBottom = box.lowerLeftY + (pageHeight - (line.bbox.bottom * scaleY))
                            val textHeight = (line.bbox.height() * scaleY).coerceAtLeast(8f)
                            val fontSize = textHeight.coerceIn(8f, 18f)

                            stream.beginText()
                            stream.setFont(PDType1Font.HELVETICA, fontSize)
                            stream.newLineAtOffset(pdfLeft, pdfBottom)
                            stream.showText(sanitizePdfText(line.text))
                            stream.endText()
                        }
                    }

                    val processed = pageIndex + 1
                    if (processed == pageCount || processed % progressChunk == 0) {
                        onProgress?.invoke(
                            PdfOcrProgress(
                                stage = "Embedding OCR text layer",
                                current = processed,
                                total = pageCount
                            )
                        )
                    }
                }

                outDoc.save(outputFile)
            }
        }
    }

    private fun writeSearchablePdfFromImage(
        bitmap: Bitmap,
        outputFile: File,
        lines: List<OcrLine>
    ) {
        PDDocument().use { document ->
            val page = PDPage(
                PDRectangle(
                    bitmap.width.toFloat().coerceAtLeast(1f),
                    bitmap.height.toFloat().coerceAtLeast(1f)
                )
            )
            document.addPage(page)

            val image = LosslessFactory.createFromImage(document, bitmap)
            PDPageContentStream(document, page).use { stream ->
                stream.drawImage(
                    image,
                    0f,
                    0f,
                    bitmap.width.toFloat(),
                    bitmap.height.toFloat()
                )
            }

            PDPageContentStream(
                document,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
            ).use { stream ->
                stream.setRenderingMode(RenderingMode.NEITHER)
                stream.setFont(PDType1Font.HELVETICA, 10f)

                lines.forEach { line ->
                    val pdfLeft = line.bbox.left.toFloat().coerceAtLeast(0f)
                    val pdfBottom = (bitmap.height - line.bbox.bottom).toFloat().coerceAtLeast(0f)
                    val fontSize = line.bbox.height().toFloat().coerceIn(8f, 24f)

                    stream.beginText()
                    stream.setFont(PDType1Font.HELVETICA, fontSize)
                    stream.newLineAtOffset(pdfLeft, pdfBottom)
                    stream.showText(sanitizePdfText(line.text))
                    stream.endText()
                }
            }

            document.save(outputFile)
        }
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

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
