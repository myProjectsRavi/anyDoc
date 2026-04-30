package com.docforge.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val base = outputBaseName.ifBlank { "scan_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val outputFiles = mutableListOf<File>()
        var totalBytes = 0L

        imageUris.forEachIndexed { index, uri ->
            val bitmap = decodeBitmap(uri) ?: error("Failed to decode page ${index + 1}: $uri")
            val extension = if (format == ScanImageFormat.JPG) "jpg" else "png"
            val outputFile = File(outputDir, "${base}_p${index + 1}.$extension")

            FileOutputStream(outputFile).use { stream ->
                val success = bitmap.compress(
                    if (format == ScanImageFormat.JPG) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG,
                    jpegQuality.coerceIn(10, 100),
                    stream
                )
                require(success) { "Failed to write page ${index + 1}." }
            }
            bitmap.recycle()

            outputFiles += outputFile
            totalBytes += outputFile.length()
        }

        val zipFile = if (zipBundle) {
            val zip = File(outputDir, "${base}_${format.name.lowercase()}_bundle.zip")
            ZipOutputStream(FileOutputStream(zip)).use { zipOut ->
                outputFiles.forEach { imageFile ->
                    FileInputStream(imageFile).use { input ->
                        zipOut.putNextEntry(ZipEntry(imageFile.name))
                        input.copyTo(zipOut, bufferSize = 8 * 1024)
                        zipOut.closeEntry()
                    }
                }
            }
            zip
        } else {
            null
        }

        ScanImageExportResult(
            outputFiles = outputFiles,
            outputSizeBytes = totalBytes + (zipFile?.length() ?: 0L),
            bundleZipFile = zipFile
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return decodeBitmapConstrained(context, uri, maxLongEdge = 2000)
    }
}
