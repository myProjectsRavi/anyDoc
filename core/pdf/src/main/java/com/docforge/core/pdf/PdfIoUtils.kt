package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.docforge.core.domain.io.ActiveTempFileRegistry
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

private const val FILE_CHANNEL_COPY_CHUNK_BYTES = 8L * 1024L * 1024L
private val SAFE_EXTENSION_REGEX = Regex("[a-z0-9]{1,8}")

internal fun Context.copyUriToCacheFile(
    uri: Uri,
    prefix: String,
    suffix: String = guessTempSuffix(uri, defaultSuffix = ".bin"),
    keepRegistered: Boolean = false
): File {
    val tempFile = File.createTempFile(prefix, suffix, cacheDir)
    ActiveTempFileRegistry.register(tempFile)
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
        ActiveTempFileRegistry.unregister(tempFile)
        tempFile.delete()
        throw t
    } finally {
        if (!keepRegistered) {
            ActiveTempFileRegistry.unregister(tempFile)
        }
    }
}

internal inline fun <T> Context.withUriCopiedToCacheFile(
    uri: Uri,
    prefix: String,
    suffix: String = guessTempSuffix(uri, defaultSuffix = ".bin"),
    block: (File) -> T
): T {
    val tempFile = copyUriToCacheFile(
        uri = uri,
        prefix = prefix,
        suffix = suffix,
        keepRegistered = true
    )
    return try {
        block(tempFile)
    } finally {
        ActiveTempFileRegistry.unregister(tempFile)
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


internal data class StagedOutputResult<T>(
    val outputFile: File,
    val value: T
)

/**
 * Writes a final output through a same-directory staging file.
 *
 * The user-visible destination is created only after [block] returns successfully. If the
 * operation fails or is cancelled, the staging file is deleted and no partial final file is
 * published. Publishing uses a same-filesystem move and never replaces an existing output.
 */
internal inline fun <T> withStagedOutputFile(
    directory: File,
    baseName: String,
    extension: String,
    block: (stagedFile: File) -> T
): StagedOutputResult<T> {
    require(directory.exists() || directory.mkdirs()) {
        "Unable to create output directory: ${directory.absolutePath}"
    }

    val stagedFile = File.createTempFile(".anydoc_stage_", ".part", directory)
    return try {
        val value = block(stagedFile)
        require(stagedFile.isFile) { "Staged output was not created." }
        val outputFile = moveStagedFileWithoutOverwrite(
            stagedFile = stagedFile,
            directory = directory,
            baseName = baseName,
            extension = extension
        )
        StagedOutputResult(outputFile = outputFile, value = value)
    } finally {
        if (stagedFile.exists()) {
            stagedFile.delete()
        }
    }
}

private fun moveStagedFileWithoutOverwrite(
    stagedFile: File,
    directory: File,
    baseName: String,
    extension: String
): File {
    var counter = 0
    while (counter < 10_000) {
        val candidate = if (counter == 0) {
            File(directory, "$baseName.$extension")
        } else {
            File(directory, "${baseName}_$counter.$extension")
        }

        if (candidate.exists()) {
            counter++
            continue
        }

        try {
            Files.move(stagedFile.toPath(), candidate.toPath())
            return candidate
        } catch (_: FileAlreadyExistsException) {
            // Another writer won the race after exists(); choose the next non-conflicting name.
            counter++
        }
    }

    error("Unable to allocate a non-conflicting output name for $baseName.$extension")
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
