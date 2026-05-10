package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels

private const val FILE_CHANNEL_COPY_CHUNK_BYTES = 8L * 1024L * 1024L
private val SAFE_EXTENSION_REGEX = Regex("[a-z0-9]{1,8}")

internal fun Context.copyUriToCacheFile(
    uri: Uri,
    prefix: String,
    suffix: String = guessTempSuffix(uri, defaultSuffix = ".bin")
): File {
    val tempFile = File.createTempFile(prefix, suffix, cacheDir)
    return try {
        val input = contentResolver.openInputStream(uri) ?: error("Unable to open input: $uri")
        input.use { source ->
            Channels.newChannel(source).use { sourceChannel ->
                FileOutputStream(tempFile).channel.use { targetChannel ->
                    var position = 0L
                    while (true) {
                        val transferred = targetChannel.transferFrom(
                            sourceChannel,
                            position,
                            FILE_CHANNEL_COPY_CHUNK_BYTES
                        )
                        if (transferred <= 0L) break
                        position += transferred
                    }
                }
            }
        }
        tempFile
    } catch (t: Throwable) {
        tempFile.delete()
        throw t
    }
}

internal inline fun <T> Context.withUriCopiedToCacheFile(
    uri: Uri,
    prefix: String,
    suffix: String = guessTempSuffix(uri, defaultSuffix = ".bin"),
    block: (File) -> T
): T {
    val tempFile = copyUriToCacheFile(uri = uri, prefix = prefix, suffix = suffix)
    return try {
        block(tempFile)
    } finally {
        tempFile.delete()
    }
}

internal fun guessTempSuffix(uri: Uri, defaultSuffix: String): String {
    val extension = uri.lastPathSegment
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        .orEmpty()

    val safeExt = extension.takeIf { it.matches(SAFE_EXTENSION_REGEX) }
    return if (safeExt == null) defaultSuffix else ".$safeExt"
}

private fun pdfMemoryUsageSetting(): MemoryUsageSetting = MemoryUsageSetting.setupTempFileOnly()

internal fun loadPdfDocument(file: File): PDDocument {
    return PDDocument.load(file, pdfMemoryUsageSetting())
}

internal fun loadPdfDocument(file: File, password: String): PDDocument {
    return PDDocument.load(file, password, pdfMemoryUsageSetting())
}

fun resolveNonConflictingFile(directory: File, baseName: String, extension: String): File {
    val candidate = File(directory, "$baseName.$extension")
    if (!candidate.exists()) return candidate
    var counter = 1
    while (true) {
        val numbered = File(directory, "${baseName}_$counter.$extension")
        if (!numbered.exists()) return numbered
        counter++
    }
}

/**
 * Imports a page from one PDDocument into another, preserving rotation,
 * media box, crop box, and resources.
 *
 * Shared utility extracted from 7+ tool classes (Sprint 6 — LOW-2).
 */
internal fun importPageFull(outDoc: com.tom_roush.pdfbox.pdmodel.PDDocument, sourcePage: com.tom_roush.pdfbox.pdmodel.PDPage): com.tom_roush.pdfbox.pdmodel.PDPage {
    val imported = outDoc.importPage(sourcePage)
    imported.rotation = sourcePage.rotation
    imported.mediaBox = sourcePage.mediaBox
    imported.cropBox = sourcePage.cropBox
    imported.resources = sourcePage.resources
    return imported
}
