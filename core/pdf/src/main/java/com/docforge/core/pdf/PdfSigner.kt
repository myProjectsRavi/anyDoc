package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

data class PdfSignaturePlacement(
    val targetPageOneBased: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float
)

class PdfSigner(
    private val context: Context
) {

    suspend fun sign(
        inputUri: Uri,
        outputName: String,
        signatureBitmap: Bitmap,
        targetPageOneBased: Int,
        xRatio: Float,
        yRatio: Float,
        widthRatio: Float
    ): PdfCreationResult {
        return signMultiple(
            inputUri = inputUri,
            outputName = outputName,
            signatureBitmap = signatureBitmap,
            placements = listOf(
                PdfSignaturePlacement(
                    targetPageOneBased = targetPageOneBased,
                    xRatio = xRatio,
                    yRatio = yRatio,
                    widthRatio = widthRatio
                )
            )
        )
    }

    suspend fun signMultiple(
        inputUri: Uri,
        outputName: String,
        signatureBitmap: Bitmap,
        placements: List<PdfSignaturePlacement>
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(placements.isNotEmpty()) { "No signature placements provided." }
        require(signatureBitmap.width > 0 && signatureBitmap.height > 0) { "Invalid signature bitmap." }
        val checkCancelled = { coroutineContext.ensureActive() }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "signed_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(outputDir, "$sanitized.pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_sign_src_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { sourceDoc ->
                require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

                placements.forEach { placement ->
                    require(placement.targetPageOneBased in 1..sourceDoc.numberOfPages) {
                        "Target page out of bounds: ${placement.targetPageOneBased}"
                    }
                }

                val placementsByPage = placements.groupBy { placement -> placement.targetPageOneBased }

                PDDocument().use { outDoc ->
                    val signatureImage = LosslessFactory.createFromImage(outDoc, signatureBitmap)

                    repeat(sourceDoc.numberOfPages) { pageIndex ->
                        checkCancelled()
                        val sourcePage = sourceDoc.getPage(pageIndex)
                        val importedPage = importPage(outDoc, sourcePage)

                        val pagePlacements = placementsByPage[pageIndex + 1].orEmpty()
                        if (pagePlacements.isNotEmpty()) {
                            PDPageContentStream(
                                outDoc,
                                importedPage,
                                PDPageContentStream.AppendMode.APPEND,
                                true,
                                true
                            ).use { stream ->
                                pagePlacements.forEach { placement ->
                                    checkCancelled()
                                    drawSignature(
                                        stream = stream,
                                        page = importedPage,
                                        signatureBitmap = signatureBitmap,
                                        signatureImage = signatureImage,
                                        xRatio = placement.xRatio,
                                        yRatio = placement.yRatio,
                                        widthRatio = placement.widthRatio
                                    )
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
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_sign_count_", suffix = ".pdf") { sourceFile ->
                loadPdfDocument(sourceFile).use { sourceDoc -> sourceDoc.numberOfPages }
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

    private fun drawSignature(
        stream: PDPageContentStream,
        page: PDPage,
        signatureBitmap: Bitmap,
        signatureImage: com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject,
        xRatio: Float,
        yRatio: Float,
        widthRatio: Float
    ) {
        val box = page.cropBox ?: page.mediaBox

        val pageWidth = box.width.coerceAtLeast(1f)
        val pageHeight = box.height.coerceAtLeast(1f)

        val clampedWidthRatio = widthRatio.coerceIn(0.1f, 0.8f)
        val targetWidth = pageWidth * clampedWidthRatio
        val targetHeight = (targetWidth * signatureBitmap.height / signatureBitmap.width).coerceAtLeast(1f)

        val maxLeft = (pageWidth - targetWidth).coerceAtLeast(0f)
        val maxTopFromTop = (pageHeight - targetHeight).coerceAtLeast(0f)

        val left = box.lowerLeftX + (maxLeft * xRatio.coerceIn(0f, 1f))
        val topFromTop = maxTopFromTop * yRatio.coerceIn(0f, 1f)
        val bottom = box.lowerLeftY + (pageHeight - targetHeight - topFromTop).coerceAtLeast(0f)

        stream.drawImage(signatureImage, left, bottom, targetWidth, targetHeight)
    }
}
