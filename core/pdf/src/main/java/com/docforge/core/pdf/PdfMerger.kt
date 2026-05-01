package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.coroutineContext

class PdfMerger(
    private val context: Context
) {
    suspend fun merge(
        inputUris: List<Uri>,
        outputName: String,
        options: PdfMergeOptions = PdfMergeOptions()
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(inputUris.isNotEmpty()) { "Select at least one source file." }
        val checkCancelled = { coroutineContext.ensureActive() }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "merged_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

        val sourceBookmarks = mutableListOf<PdfSourceBookmark>()

        PDDocument().use { mergedDoc ->
            var outputPageNumber = 1

            inputUris.forEach { uri ->
                checkCancelled()
                sourceBookmarks += PdfSourceBookmark(
                    label = resolveSourceLabel(uri),
                    startPageOneBased = outputPageNumber
                )

                when (resolveInputType(uri)) {
                    MergeInputType.PDF -> {
                        context.withUriCopiedToCacheFile(uri, prefix = "docforge_merge_pdf_", suffix = ".pdf") { sourceFile ->
                            loadPdfDocument(sourceFile).use { sourceDoc ->
                                require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages: $uri" }

                                repeat(sourceDoc.numberOfPages) { pageIndex ->
                                    checkCancelled()
                                    val sourcePage = sourceDoc.getPage(pageIndex)
                                    val imported = mergedDoc.importPage(sourcePage)
                                    imported.rotation = sourcePage.rotation
                                    imported.mediaBox = sourcePage.mediaBox
                                    imported.cropBox = sourcePage.cropBox
                                    imported.resources = sourcePage.resources
                                    outputPageNumber += 1
                                }
                            }
                        }
                    }

                    MergeInputType.IMAGE -> {
                        val bitmap = decodeBitmapConstrained(context, uri, maxLongEdge = 2200)
                            ?: error("Failed to decode image source: $uri")
                        appendImagePage(
                            document = mergedDoc,
                            bitmap = bitmap,
                            pageSizeMode = options.pageSizeMode
                        )
                        bitmap.recycle()
                        outputPageNumber += 1
                    }

                    MergeInputType.UNSUPPORTED -> {
                        error("Unsupported merge source. Only PDF and image files are allowed: $uri")
                    }
                }
            }

            applyMetadataAndBookmarks(
                document = mergedDoc,
                options = options,
                sourceBookmarks = sourceBookmarks
            )
            mergedDoc.save(outputFile)

            PdfCreationResult(
                outputFile = outputFile,
                pageCount = outputPageNumber - 1,
                outputSizeBytes = outputFile.length()
            )
        }
    }

    private fun applyMetadataAndBookmarks(
        document: PDDocument,
        options: PdfMergeOptions,
        sourceBookmarks: List<PdfSourceBookmark>
    ) {
        val shouldSetMetadata = options.title.isNotBlank() || options.author.isNotBlank() || options.subject.isNotBlank()
        val shouldSetBookmarks = options.addSourceBookmarks && sourceBookmarks.isNotEmpty()
        if (!shouldSetMetadata && !shouldSetBookmarks) return

        if (shouldSetMetadata) {
            val info = document.documentInformation
            if (options.title.isNotBlank()) {
                info.title = options.title
            }
            if (options.author.isNotBlank()) {
                info.author = options.author
            }
            if (options.subject.isNotBlank()) {
                info.subject = options.subject
            }
            document.documentInformation = info
        }

        if (shouldSetBookmarks) {
            val outline = PDDocumentOutline()
            sourceBookmarks.forEach { bookmark ->
                val targetIndex = (bookmark.startPageOneBased - 1).coerceIn(0, document.numberOfPages - 1)
                val destination = PDPageFitDestination().apply {
                    page = document.getPage(targetIndex)
                }
                val item = PDOutlineItem().apply {
                    title = bookmark.label
                    setDestination(destination)
                }
                outline.addLast(item)
            }
            outline.openNode()
            document.documentCatalog.documentOutline = outline
        }
    }

    private fun resolveInputType(uri: Uri): MergeInputType {
        val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.getDefault())
        if (mimeType == "application/pdf") return MergeInputType.PDF
        if (mimeType.startsWith("image/")) return MergeInputType.IMAGE

        val extension = uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.getDefault())
            .orEmpty()

        if (extension == "pdf") return MergeInputType.PDF
        if (extension in setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "tif", "tiff")) {
            return MergeInputType.IMAGE
        }
        return MergeInputType.UNSUPPORTED
    }

    private fun appendImagePage(
        document: PDDocument,
        bitmap: Bitmap,
        pageSizeMode: PdfMergePageSizeMode
    ) {
        val sourceWidth = bitmap.width.coerceAtLeast(1).toFloat()
        val sourceHeight = bitmap.height.coerceAtLeast(1).toFloat()
        val pageSize = resolveOutputPageSize(pageSizeMode, sourceWidth, sourceHeight)

        val page = PDPage(pageSize)
        document.addPage(page)

        val image = LosslessFactory.createFromImage(document, bitmap)

        val scale = minOf(pageSize.width / sourceWidth, pageSize.height / sourceHeight)
        val targetWidth = (sourceWidth * scale).coerceAtLeast(1f)
        val targetHeight = (sourceHeight * scale).coerceAtLeast(1f)
        val left = ((pageSize.width - targetWidth) / 2f).coerceAtLeast(0f)
        val bottom = ((pageSize.height - targetHeight) / 2f).coerceAtLeast(0f)

        PDPageContentStream(document, page).use { stream ->
            stream.drawImage(image, left, bottom, targetWidth, targetHeight)
        }
    }

    private fun resolveOutputPageSize(
        mode: PdfMergePageSizeMode,
        sourceWidth: Float,
        sourceHeight: Float
    ): PDRectangle {
        return when (mode) {
            PdfMergePageSizeMode.KEEP_SOURCE -> PDRectangle(sourceWidth, sourceHeight)
            PdfMergePageSizeMode.A4_FIT -> PDRectangle.A4
            PdfMergePageSizeMode.LETTER_FIT -> PDRectangle.LETTER
        }
    }

    private fun resolveSourceLabel(uri: Uri): String {
        val raw = uri.lastPathSegment?.substringAfterLast('/')?.trim().orEmpty()
        return if (raw.isBlank()) {
            "Source"
        } else {
            raw
        }
    }
}

private enum class MergeInputType {
    PDF,
    IMAGE,
    UNSUPPORTED
}

data class PdfMergeOptions(
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val addSourceBookmarks: Boolean = true,
    val pageSizeMode: PdfMergePageSizeMode = PdfMergePageSizeMode.KEEP_SOURCE
)

enum class PdfMergePageSizeMode {
    KEEP_SOURCE,
    A4_FIT,
    LETTER_FIT
}

private data class PdfSourceBookmark(
    val label: String,
    val startPageOneBased: Int
)
