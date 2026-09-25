package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.min

data class PdfSplitResult(
    val outputFiles: List<File>,
    val outputSizeBytes: Long,
    val totalPagesExported: Int
)

class PdfSplitter(
    private val context: Context
) {
    suspend fun splitByRange(
        inputUri: Uri,
        outputName: String,
        startPageOneBased: Int,
        endPageOneBased: Int
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        withLoadedSourceDocument(inputUri) { sourceDoc ->
            require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }
            require(startPageOneBased in 1..sourceDoc.numberOfPages) { "Start page out of bounds." }
            require(endPageOneBased in 1..sourceDoc.numberOfPages) { "End page out of bounds." }
            require(startPageOneBased <= endPageOneBased) { "Start page must be <= end page." }

            val outputDir = outputDirectory()
            val sanitized = sanitizeName(outputName, "split")
            val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

            PDDocument().use { outDoc ->
                for (pageOneBased in startPageOneBased..endPageOneBased) {
                    checkCancelled()
                    importPage(outDoc, sourceDoc.getPage(pageOneBased - 1))
                }
                outDoc.save(outputFile)
            }

            PdfCreationResult(
                outputFile = outputFile,
                pageCount = endPageOneBased - startPageOneBased + 1,
                outputSizeBytes = outputFile.length()
            )
        }
    }

    suspend fun extractPages(
        inputUri: Uri,
        outputBaseName: String,
        pagesOneBased: List<Int>
    ): PdfSplitResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        withLoadedSourceDocument(inputUri) { sourceDoc ->
            require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }
            require(pagesOneBased.isNotEmpty()) { "No pages selected for extraction." }

            val requested = pagesOneBased
                .distinct()
                .sorted()
                .onEach { page ->
                    require(page in 1..sourceDoc.numberOfPages) { "Page $page is out of bounds." }
                }

            val outputDir = outputDirectory()
            val sanitizedBase = sanitizeName(outputBaseName, "extract")

            val createdFiles = mutableListOf<File>()
            var bytes = 0L

            requested.forEach { pageOneBased ->
                checkCancelled()
                val out = resolveNonConflictingFile(outputDir, "${sanitizedBase}_p$pageOneBased", "pdf")
                PDDocument().use { outDoc ->
                    importPage(outDoc, sourceDoc.getPage(pageOneBased - 1))
                    outDoc.save(out)
                }
                createdFiles += out
                bytes += out.length()
            }

            PdfSplitResult(
                outputFiles = createdFiles,
                outputSizeBytes = bytes,
                totalPagesExported = requested.size
            )
        }
    }

    suspend fun splitEveryNPages(
        inputUri: Uri,
        outputBaseName: String,
        pagesPerChunk: Int
    ): PdfSplitResult = withContext(Dispatchers.IO) {
        require(pagesPerChunk > 0) { "Pages per split must be greater than 0." }
        val checkCancelled = { coroutineContext.ensureActive() }

        withLoadedSourceDocument(inputUri) { sourceDoc ->
            require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

            val outputDir = outputDirectory()
            val sanitizedBase = sanitizeName(outputBaseName, "split_n")
            val createdFiles = mutableListOf<File>()
            var bytes = 0L

            var startPageOneBased = 1
            while (startPageOneBased <= sourceDoc.numberOfPages) {
                checkCancelled()
                val endPageOneBased = min(startPageOneBased + pagesPerChunk - 1, sourceDoc.numberOfPages)
                val out = resolveNonConflictingFile(outputDir, "${sanitizedBase}_${startPageOneBased}_${endPageOneBased}", "pdf")

                PDDocument().use { outDoc ->
                    for (pageOneBased in startPageOneBased..endPageOneBased) {
                        checkCancelled()
                        importPage(outDoc, sourceDoc.getPage(pageOneBased - 1))
                    }
                    outDoc.save(out)
                }

                createdFiles += out
                bytes += out.length()
                startPageOneBased = endPageOneBased + 1
            }

            PdfSplitResult(
                outputFiles = createdFiles,
                outputSizeBytes = bytes,
                totalPagesExported = sourceDoc.numberOfPages
            )
        }
    }

    suspend fun splitByBookmarks(
        inputUri: Uri,
        outputBaseName: String
    ): PdfSplitResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        val outputDir = outputDirectory()
        val sanitizedBase = sanitizeName(outputBaseName, "split_bookmarks")

        withLoadedSourceDocument(inputUri) { sourceDoc ->
            val totalPages = sourceDoc.numberOfPages
            require(totalPages > 0) { "Input PDF has no pages." }

            val boundaries = resolveTopLevelBookmarkBoundaries(sourceDoc)
            require(boundaries.isNotEmpty()) { "No bookmark boundaries found in PDF outline." }

            val createdFiles = mutableListOf<File>()
            var bytes = 0L

            boundaries.forEachIndexed { index, boundary ->
                checkCancelled()
                val nextStart = boundaries.getOrNull(index + 1)?.startPageOneBased ?: (totalPages + 1)
                val endPage = (nextStart - 1).coerceAtMost(totalPages)
                if (endPage < boundary.startPageOneBased) return@forEachIndexed

                val titlePart = sanitizeName(boundary.title, "bookmark_${index + 1}")
                val outFile = File(
                    outputDir,
                    "${sanitizedBase}_${index + 1}_${titlePart}_${boundary.startPageOneBased}_$endPage.pdf"
                )

                PDDocument().use { outDoc ->
                    for (pageIndex in (boundary.startPageOneBased - 1)..(endPage - 1)) {
                        checkCancelled()
                        importPage(outDoc, sourceDoc.getPage(pageIndex))
                    }
                    outDoc.save(outFile)
                }

                createdFiles += outFile
                bytes += outFile.length()
            }

            PdfSplitResult(
                outputFiles = createdFiles,
                outputSizeBytes = bytes,
                totalPagesExported = totalPages
            )
        }
    }

    suspend fun reorderPages(
        inputUri: Uri,
        outputName: String,
        orderedPagesOneBased: List<Int>
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(orderedPagesOneBased.isNotEmpty()) { "No pages specified for reorder." }
        val checkCancelled = { coroutineContext.ensureActive() }

        withLoadedSourceDocument(inputUri) { sourceDoc ->
            require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

            orderedPagesOneBased.forEach { page ->
                require(page in 1..sourceDoc.numberOfPages) { "Page $page is out of bounds." }
            }

            val outputDir = outputDirectory()
            val sanitized = sanitizeName(outputName, "reorder")
            val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

            PDDocument().use { outDoc ->
                orderedPagesOneBased.forEach { pageOneBased ->
                    checkCancelled()
                    importPage(outDoc, sourceDoc.getPage(pageOneBased - 1))
                }
                outDoc.save(outputFile)
            }

            PdfCreationResult(
                outputFile = outputFile,
                pageCount = orderedPagesOneBased.size,
                outputSizeBytes = outputFile.length()
            )
        }
    }

    suspend fun deletePages(
        inputUri: Uri,
        outputName: String,
        pagesToDeleteOneBased: List<Int>
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(pagesToDeleteOneBased.isNotEmpty()) { "No pages specified for deletion." }
        val checkCancelled = { coroutineContext.ensureActive() }

        withLoadedSourceDocument(inputUri) { sourceDoc ->
            require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

            val deleteSet = pagesToDeleteOneBased.toSet()
            deleteSet.forEach { page ->
                require(page in 1..sourceDoc.numberOfPages) { "Page $page is out of bounds." }
            }

            val keptPages = (1..sourceDoc.numberOfPages).filterNot { page -> page in deleteSet }
            require(keptPages.isNotEmpty()) { "At least one page must remain after deletion." }

            val outputDir = outputDirectory()
            val sanitized = sanitizeName(outputName, "delete")
            val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

            PDDocument().use { outDoc ->
                keptPages.forEach { pageOneBased ->
                    checkCancelled()
                    importPage(outDoc, sourceDoc.getPage(pageOneBased - 1))
                }
                outDoc.save(outputFile)
            }

            PdfCreationResult(
                outputFile = outputFile,
                pageCount = keptPages.size,
                outputSizeBytes = outputFile.length()
            )
        }
    }

    suspend fun rotatePages(
        inputUri: Uri,
        outputName: String,
        pagesToRotateOneBased: List<Int>,
        degreesClockwise: Int
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(pagesToRotateOneBased.isNotEmpty()) { "No pages specified for rotation." }
        require(degreesClockwise in setOf(90, 180, 270)) { "Rotation must be 90, 180, or 270 degrees." }
        val checkCancelled = { coroutineContext.ensureActive() }

        withLoadedSourceDocument(inputUri) { sourceDoc ->
            require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

            val rotateSet = pagesToRotateOneBased.toSet()
            rotateSet.forEach { page ->
                require(page in 1..sourceDoc.numberOfPages) { "Page $page is out of bounds." }
            }

            val outputDir = outputDirectory()
            val sanitized = sanitizeName(outputName, "rotate")
            val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

            PDDocument().use { outDoc ->
                for (pageOneBased in 1..sourceDoc.numberOfPages) {
                    checkCancelled()
                    val sourcePage = sourceDoc.getPage(pageOneBased - 1)
                    val imported = importPage(outDoc, sourcePage)
                    if (pageOneBased in rotateSet) {
                        imported.rotation = ((sourcePage.rotation + degreesClockwise) % 360 + 360) % 360
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

    suspend fun applyWorkspaceEdits(
        inputUri: Uri,
        outputName: String,
        visualOrderOneBased: List<Int>,
        rotationDegreesBySourcePage: Map<Int, Int>
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(visualOrderOneBased.isNotEmpty()) { "Workspace has no pages to save." }
        val checkCancelled = { coroutineContext.ensureActive() }

        withLoadedSourceDocument(inputUri) { sourceDoc ->
            require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

            visualOrderOneBased.forEach { page ->
                require(page in 1..sourceDoc.numberOfPages) { "Page $page is out of bounds." }
            }

            rotationDegreesBySourcePage.forEach { (page, degrees) ->
                require(page in 1..sourceDoc.numberOfPages) { "Rotation page $page is out of bounds." }
                require(degrees % 90 == 0) { "Rotation degrees for page $page must be a multiple of 90." }
            }

            val outputDir = outputDirectory()
            val sanitized = sanitizeName(outputName, "workspace")
            val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

            PDDocument().use { outDoc ->
                visualOrderOneBased.forEach { pageOneBased ->
                    checkCancelled()
                    val sourcePage = sourceDoc.getPage(pageOneBased - 1)
                    val imported = importPage(outDoc, sourcePage)
                    val extraRotation = rotationDegreesBySourcePage[pageOneBased] ?: 0
                    if (extraRotation != 0) {
                        imported.rotation = normalizeRotation(sourcePage.rotation + extraRotation)
                    }
                }
                outDoc.save(outputFile)
            }

            PdfCreationResult(
                outputFile = outputFile,
                pageCount = visualOrderOneBased.size,
                outputSizeBytes = outputFile.length()
            )
        }
    }

    suspend fun getPageCount(inputUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            withLoadedSourceDocument(inputUri) { sourceDoc -> sourceDoc.numberOfPages }
        }.getOrDefault(0)
    }

    private inline fun <T> withLoadedSourceDocument(inputUri: Uri, block: (PDDocument) -> T): T {
        return context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_split_src_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use(block)
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

    private fun outputDirectory(): File {
        return DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
    }

    private fun sanitizeName(raw: String, fallbackPrefix: String): String {
        return raw.ifBlank { "${fallbackPrefix}_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    private fun normalizeRotation(value: Int): Int {
        return ((value % 360) + 360) % 360
    }

    private fun resolveTopLevelBookmarkBoundaries(document: PDDocument): List<BookmarkBoundary> {
        val outline = document.documentCatalog.documentOutline ?: return emptyList()
        if (!outline.hasChildren()) return emptyList()

        val pageTree = document.pages
        val boundaries = mutableListOf<BookmarkBoundary>()

        outline.children().forEach { item: PDOutlineItem ->
            runCatching {
                val page = item.findDestinationPage(document) ?: return@runCatching
                val pageIndex = pageTree.indexOf(page)
                if (pageIndex >= 0) {
                    boundaries += BookmarkBoundary(
                        title = item.title?.trim().orEmpty().ifBlank { "Bookmark_${pageIndex + 1}" },
                        startPageOneBased = pageIndex + 1
                    )
                }
            }
        }

        return boundaries
            .distinctBy { boundary -> boundary.startPageOneBased }
            .sortedBy { boundary -> boundary.startPageOneBased }
    }
}

private data class BookmarkBoundary(
    val title: String,
    val startPageOneBased: Int
)
