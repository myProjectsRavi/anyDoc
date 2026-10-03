package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ScanImageFormat {
    JPG,
    PNG
}

data class ScanImageExportResult(
    val outputFiles: List<File>,
    val outputSizeBytes: Long,
    val bundleZipFile: File? = null
)

class ScanImageExporter(
    private val context: Context
) {

    suspend fun export(
        imageUris: List<Uri>,
        outputBaseName: String,
        format: ScanImageFormat,
        jpegQuality: Int = 98,
        zipBundle: Boolean = false
    ): ScanImageExportResult = withContext(Dispatchers.IO) {
        require(imageUris.isNotEmpty()) { "No scan pages available for export." }
        val checkCancelled = { coroutineContext.ensureActive() }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val base = outputBaseName.ifBlank { "scan_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val extension = if (format == ScanImageFormat.JPG) "jpg" else "png"

        if (zipBundle) {
            val stagedZip = withStagedOutputFile(
                directory = outputDir,
                baseName = "${base}_${format.name.lowercase()}_bundle",
                extension = "zip"
            ) { stagedFile ->
                ZipOutputStream(FileOutputStream(stagedFile)).use { zipOut ->
                    imageUris.forEachIndexed { index, uri ->
                        checkCancelled()
                        zipOut.putNextEntry(ZipEntry("${base}_p${index + 1}.$extension"))
                        try {
                            encodeScanPage(
                                uri = uri,
                                pageIndex = index,
                                format = format,
                                jpegQuality = jpegQuality,
                                output = zipOut
                            )
                        } finally {
                            zipOut.closeEntry()
                        }
                    }
                }
                Unit
            }

            return@withContext ScanImageExportResult(
                outputFiles = emptyList(),
                outputSizeBytes = stagedZip.outputFile.length(),
                bundleZipFile = stagedZip.outputFile
            )
        }

        val pageRequests = imageUris.mapIndexed { index, uri ->
            StagedOutputRequest(
                baseName = "${base}_p${index + 1}",
                extension = extension
            ) { stagedFile ->
                checkCancelled()
                FileOutputStream(stagedFile).use { stream ->
                    encodeScanPage(
                        uri = uri,
                        pageIndex = index,
                        format = format,
                        jpegQuality = jpegQuality,
                        output = stream
                    )
                }
                Unit
            }
        }

        val stagedPages = withStagedOutputFiles(
            directory = outputDir,
            requests = pageRequests
        )
        val outputFiles = stagedPages.map { it.outputFile }

        ScanImageExportResult(
            outputFiles = outputFiles,
            outputSizeBytes = outputFiles.sumOf(File::length),
            bundleZipFile = null
        )
    }

    private fun encodeScanPage(
        uri: Uri,
        pageIndex: Int,
        format: ScanImageFormat,
        jpegQuality: Int,
        output: OutputStream
    ) {
        val bitmap = decodeBitmap(uri) ?: error("Failed to decode page ${pageIndex + 1}: $uri")
        try {
            val success = bitmap.compress(
                if (format == ScanImageFormat.JPG) {
                    Bitmap.CompressFormat.JPEG
                } else {
                    Bitmap.CompressFormat.PNG
                },
                jpegQuality.coerceIn(10, 100),
                output
            )
            require(success) { "Failed to write page ${pageIndex + 1}." }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return decodeBitmapConstrained(context, uri, maxLongEdge = 2000)
    }
}
