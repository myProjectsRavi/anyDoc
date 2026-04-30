package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.view.translation.TranslationContext
import android.view.translation.TranslationManager
import android.view.translation.TranslationRequest
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponse
import android.view.translation.TranslationSpec
import android.view.translation.Translator
import androidx.annotation.RequiresApi
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
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

data class PdfTranslationResult(
    val outputFile: File,
    val pageCount: Int,
    val translatedLineCount: Int,
    val outputSizeBytes: Long
)

data class PdfTranslationProgress(
    val stage: String,
    val current: Int,
    val total: Int
)

private data class TranslatableLine(
    val pageOneBased: Int,
    val text: String,
    val bbox: Rect,
    val bitmapWidth: Int,
    val bitmapHeight: Int
)

class PdfTranslationTool(
    private val context: Context
) {
    suspend fun translatePdfWithLayout(
        inputUri: Uri,
        outputName: String,
        sourceLanguageTag: String,
        targetLanguageTag: String,
        pageProgressChunk: Int = 2,
        translationBatchSize: Int = 24,
        onProgress: ((PdfTranslationProgress) -> Unit)? = null
    ): PdfTranslationResult = withContext(Dispatchers.IO) {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "Auto-translation requires Android 12+ with on-device translation support."
        }
        require(sourceLanguageTag.isNotBlank() && targetLanguageTag.isNotBlank()) {
            "Provide both source and target language tags (e.g., en, es, fr)."
        }

        val coroutineCtx = currentCoroutineContext()
        val progressChunk = pageProgressChunk.coerceAtLeast(1)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_translate_src_", suffix = ".pdf") { sourceFile ->
                val lines = mutableListOf<TranslatableLine>()
                var pageCount: Int

                ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        pageCount = renderer.pageCount
                        require(pageCount > 0) { "Input PDF has no pages." }
                        onProgress?.invoke(PdfTranslationProgress(stage = "Detecting text", current = 0, total = pageCount))

                        repeat(renderer.pageCount) { pageIndex ->
                            coroutineCtx.ensureActive()
                            renderer.openPage(pageIndex).use { page ->
                                val sourceWidth = page.width.coerceAtLeast(1)
                                val sourceHeight = page.height.coerceAtLeast(1)
                                val longest = max(sourceWidth, sourceHeight)
                                val scale = (1800f / longest.toFloat()).coerceAtMost(1f)
                                val renderWidth = (sourceWidth * scale).toInt().coerceAtLeast(1)
                                val renderHeight = (sourceHeight * scale).toInt().coerceAtLeast(1)

                                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                                try {
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
                                    lines += extractTranslatableLines(pageIndex + 1, bitmap, text)
                                    val processed = pageIndex + 1
                                    if (processed == pageCount || processed % progressChunk == 0) {
                                        onProgress?.invoke(
                                            PdfTranslationProgress(
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

                val translatedTexts = TranslationRuntime.translate(
                    context = context,
                    sourceLanguageTag = sourceLanguageTag,
                    targetLanguageTag = targetLanguageTag,
                    sourceTexts = lines.map { it.text },
                    batchSize = translationBatchSize,
                    onBatchProgress = { processed, total ->
                        onProgress?.invoke(
                            PdfTranslationProgress(
                                stage = "Translating text",
                                current = processed,
                                total = total
                            )
                        )
                    }
                )

                val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
                    context = context,
                    bucket = DocForgeOutputBucket.DOCUMENTS
                )
                val sanitized = outputName.ifBlank {
                    "translated_${targetLanguageTag}_${System.currentTimeMillis()}"
                }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val outputFile = File(outputDir, "$sanitized.pdf")

                writeTranslatedOverlayPdf(
                    sourceFile = sourceFile,
                    outputFile = outputFile,
                    lines = lines,
                    translatedTexts = translatedTexts,
                    pageProgressChunk = progressChunk,
                    onProgress = onProgress
                )

                PdfTranslationResult(
                    outputFile = outputFile,
                    pageCount = pageCount,
                    translatedLineCount = translatedTexts.size,
                    outputSizeBytes = outputFile.length()
                )
            }
        } finally {
            recognizer.close()
        }
    }

    private fun extractTranslatableLines(
        pageOneBased: Int,
        bitmap: Bitmap,
        text: Text
    ): List<TranslatableLine> {
        val items = mutableListOf<TranslatableLine>()
        text.textBlocks.forEach { block ->
            block.lines.forEach lineLoop@{ line ->
                val content = line.text.trim()
                val bbox = line.boundingBox
                if (content.isBlank() || bbox == null || bbox.width() <= 0 || bbox.height() <= 0) {
                    return@lineLoop
                }
                items += TranslatableLine(
                    pageOneBased = pageOneBased,
                    text = content,
                    bbox = Rect(bbox),
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                )
            }
        }
        return items
    }

    private suspend fun writeTranslatedOverlayPdf(
        sourceFile: File,
        outputFile: File,
        lines: List<TranslatableLine>,
        translatedTexts: List<String>,
        pageProgressChunk: Int,
        onProgress: ((PdfTranslationProgress) -> Unit)?
    ) {
        require(lines.size == translatedTexts.size) { "Translated output size mismatch." }
        val coroutineCtx = currentCoroutineContext()
        val pageItemsByPage = lines.indices
            .map { index -> lines[index] to translatedTexts[index] }
            .groupBy { (line, _) -> line.pageOneBased }

        loadPdfDocument(sourceFile).use { sourceDoc ->
            PDDocument().use { outDoc ->
                val pageCount = sourceDoc.numberOfPages
                val progressChunk = pageProgressChunk.coerceAtLeast(1)
                onProgress?.invoke(PdfTranslationProgress(stage = "Writing translated overlay", current = 0, total = pageCount))
                repeat(sourceDoc.numberOfPages) { pageIndex ->
                    coroutineCtx.ensureActive()
                    val sourcePage = sourceDoc.getPage(pageIndex)
                    val imported = importPage(outDoc, sourcePage)

                    val pageItems = pageItemsByPage[pageIndex + 1].orEmpty()
                    if (pageItems.isEmpty()) return@repeat

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
                        pageItems.forEach { (line, translated) ->
                            val sx = pageWidth / line.bitmapWidth.toFloat().coerceAtLeast(1f)
                            val sy = pageHeight / line.bitmapHeight.toFloat().coerceAtLeast(1f)

                            val left = box.lowerLeftX + (line.bbox.left * sx)
                            val top = box.lowerLeftY + (pageHeight - (line.bbox.top * sy))
                            val bottom = box.lowerLeftY + (pageHeight - (line.bbox.bottom * sy))
                            val width = (line.bbox.width() * sx).coerceAtLeast(12f)
                            val height = (top - bottom).coerceAtLeast(10f)

                            stream.setNonStrokingColor(255, 255, 255)
                            stream.addRect(left, bottom, width, height)
                            stream.fill()

                            val fontSize = height.coerceIn(8f, 18f)
                            val wrapped = wrapText(
                                text = sanitizePdfText(translated),
                                maxWidth = width,
                                fontSize = fontSize
                            )
                            if (wrapped.isEmpty()) return@forEach

                            stream.setNonStrokingColor(0, 0, 0)
                            var cursorY = bottom + height - fontSize
                            for (lineText in wrapped) {
                                if (cursorY < bottom) break
                                stream.beginText()
                                stream.setFont(PDType1Font.HELVETICA, fontSize)
                                stream.newLineAtOffset(left + 1f, cursorY)
                                stream.showText(lineText)
                                stream.endText()
                                cursorY -= (fontSize * 1.1f)
                            }
                        }
                    }

                    val processed = pageIndex + 1
                    if (processed == pageCount || processed % progressChunk == 0) {
                        onProgress?.invoke(
                            PdfTranslationProgress(
                                stage = "Writing translated overlay",
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

    private fun wrapText(text: String, maxWidth: Float, fontSize: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        val buffer = StringBuilder()

        words.forEach { word ->
            val candidate = if (buffer.isEmpty()) word else "${buffer} $word"
            val width = (PDType1Font.HELVETICA.getStringWidth(candidate) / 1000f) * fontSize
            if (width <= maxWidth || buffer.isEmpty()) {
                buffer.clear()
                buffer.append(candidate)
            } else {
                lines += buffer.toString()
                buffer.clear()
                buffer.append(word)
            }
        }

        if (buffer.isNotEmpty()) {
            lines += buffer.toString()
        }
        return lines.take(3)
    }

    private fun sanitizePdfText(raw: String): String {
        return raw.replace('\u0000', ' ').replace(Regex("\\s+"), " ").trim()
    }

    private fun importPage(outDoc: PDDocument, sourcePage: PDPage): PDPage {
        val imported = outDoc.importPage(sourcePage)
        imported.rotation = sourcePage.rotation
        imported.mediaBox = sourcePage.mediaBox
        imported.cropBox = sourcePage.cropBox
        imported.resources = sourcePage.resources
        return imported
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
        addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object TranslationRuntime {
    suspend fun translate(
        context: Context,
        sourceLanguageTag: String,
        targetLanguageTag: String,
        sourceTexts: List<String>,
        batchSize: Int,
        onBatchProgress: ((processed: Int, total: Int) -> Unit)?
    ): List<String> {
        if (sourceTexts.isEmpty()) return emptyList()
        val normalizedBatchSize = batchSize.coerceIn(1, 128)

        val manager = context.getSystemService(TranslationManager::class.java)
            ?: error("Translation service unavailable on this device.")

        val sourceSpec = TranslationSpec(android.icu.util.ULocale.forLanguageTag(sourceLanguageTag), TranslationSpec.DATA_FORMAT_TEXT)
        val targetSpec = TranslationSpec(android.icu.util.ULocale.forLanguageTag(targetLanguageTag), TranslationSpec.DATA_FORMAT_TEXT)
        val translationContext = TranslationContext.Builder(sourceSpec, targetSpec)
            .setTranslationFlags(TranslationContext.FLAG_LOW_LATENCY)
            .build()

        val executor = Executors.newSingleThreadExecutor()
        try {
            val translator = manager.awaitTranslator(translationContext, executor)
            try {
                val output = ArrayList<String>(sourceTexts.size)
                var processed = 0
                sourceTexts.chunked(normalizedBatchSize).forEach { chunk ->
                    val values = chunk.map { text -> TranslationRequestValue.forText(text) }
                    val request = TranslationRequest.Builder()
                        .setFlags(TranslationRequest.FLAG_TRANSLATION_RESULT)
                        .setTranslationRequestValues(values)
                        .build()

                    val response = translator.awaitResponse(request, executor)
                    if (response.translationStatus != TranslationResponse.TRANSLATION_STATUS_SUCCESS) {
                        error("Translation engine could not translate this language pair offline.")
                    }

                    val translated = response.translationResponseValues
                    chunk.indices.forEach { index ->
                        val translatedValue = translated[index]?.text?.toString()?.trim().orEmpty()
                        output += translatedValue.ifBlank { chunk[index] }
                    }
                    processed += chunk.size
                    onBatchProgress?.invoke(processed, sourceTexts.size)
                }
                return output
            } finally {
                translator.destroy()
            }
        } finally {
            executor.shutdown()
        }
    }

    private suspend fun TranslationManager.awaitTranslator(
        translationContext: TranslationContext,
        executor: java.util.concurrent.Executor
    ): Translator = suspendCancellableCoroutine { continuation ->
        createOnDeviceTranslator(translationContext, executor) { translator ->
            if (!continuation.isActive) {
                translator?.destroy()
                return@createOnDeviceTranslator
            }
            if (translator == null) {
                continuation.resumeWithException(IllegalStateException("Unable to initialize on-device translator."))
            } else {
                continuation.resume(translator)
            }
        }
    }

    private suspend fun Translator.awaitResponse(
        request: TranslationRequest,
        executor: java.util.concurrent.Executor
    ): TranslationResponse = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationSignal()
        continuation.invokeOnCancellation {
            cancellation.cancel()
        }
        translate(request, cancellation, executor) { response ->
            if (continuation.isActive) {
                continuation.resume(response)
            }
        }
    }
}
