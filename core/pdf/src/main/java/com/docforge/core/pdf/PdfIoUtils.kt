package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.docforge.core.domain.io.ActiveTempFileRegistry
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

internal const val FILE_CHANNEL_COPY_CHUNK_BYTES = 8L * 1024L * 1024L

internal suspend fun copyChunkWithCancellation(copyChunk: () -> Long): Long {
    currentCoroutineContext().ensureActive()
    return copyChunk()
}
internal const val CACHE_COPY_FREE_SPACE_RESERVE_BYTES = 32L * 1024L * 1024L
internal const val CACHE_COPY_SPACE_MULTIPLIER = 2L
private val SAFE_EXTENSION_REGEX = Regex("[a-z0-9]{1,8}")

internal suspend fun Context.copyUriToCacheFile(
    uri: Uri,
    prefix: String,
    suffix: String = guessTempSuffix(uri, defaultSuffix = ".bin"),
    keepRegistered: Boolean = false
): File {
    preflightCacheCopy(uri)
    val tempFile = File.createTempFile(prefix, suffix, cacheDir)
    ActiveTempFileRegistry.register(tempFile)
    return try {
        val input = contentResolver.openInputStream(uri) ?: error("Unable to open input: $uri")
        input.use { source ->
            Channels.newChannel(source).use { sourceChannel ->
                FileOutputStream(tempFile).channel.use { targetChannel ->
                    var position = 0L
                    while (true) {
                        val transferred = copyChunkWithCancellation {
                            targetChannel.transferFrom(
                                sourceChannel,
                                position,
                                FILE_CHANNEL_COPY_CHUNK_BYTES
                            )
                        }
                        if (transferred <= 0L) break
                        position += transferred
                        require(canContinueUnknownSizeCacheCopy(StatFs(cacheDir.absolutePath).availableBytes))
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

internal suspend inline fun <T> Context.withUriCopiedToCacheFile(
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

internal fun canContinueUnknownSizeCacheCopy(availableBytes: Long): Boolean = availableBytes >= CACHE_COPY_FREE_SPACE_RESERVE_BYTES

internal fun requiredCacheBytesForKnownInput(inputSizeBytes: Long): Long {
    if (inputSizeBytes <= 0L) return CACHE_COPY_FREE_SPACE_RESERVE_BYTES
    return if (inputSizeBytes > (Long.MAX_VALUE - CACHE_COPY_FREE_SPACE_RESERVE_BYTES) / CACHE_COPY_SPACE_MULTIPLIER) {
        Long.MAX_VALUE
    } else {
        inputSizeBytes * CACHE_COPY_SPACE_MULTIPLIER + CACHE_COPY_FREE_SPACE_RESERVE_BYTES
    }
}

private fun Context.preflightCacheCopy(uri: Uri) {
    val inputSizeBytes = queryKnownInputSize(uri) ?: return
    val availableBytes = StatFs(cacheDir.absolutePath).availableBytes
    val requiredBytes = requiredCacheBytesForKnownInput(inputSizeBytes)
    require(availableBytes >= requiredBytes) {
        "Not enough temporary storage for this file. Required ${requiredBytes / (1024L * 1024L)} MiB, available ${availableBytes / (1024L * 1024L)} MiB."
    }
}

private fun Context.queryKnownInputSize(uri: Uri): Long? {
    val cursorSize = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }
    }.getOrNull()
    if (cursorSize != null && cursorSize >= 0L) return cursorSize

    return runCatching {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0L }
        }
    }.getOrNull()
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


data class StagedOutputResult<T>(
    val outputFile: File,
    val value: T
)

class StagedOutputRequest<T>(
    val baseName: String,
    val extension: String,
    val block: suspend (stagedFile: File) -> T
)

private data class PreparedStagedOutput<T>(
    val stagedFile: File,
    val request: StagedOutputRequest<T>,
    val value: T
)

/**
 * Writes a final output through a same-directory staging file.
 *
 * The user-visible destination is created only after [block] returns successfully. If the
 * operation fails or is cancelled, the staging file is deleted and no partial final file is
 * published. Publishing uses a same-filesystem move and never replaces an existing output.
 */
suspend fun <T> withStagedOutputFile(
    directory: File,
    baseName: String,
    extension: String,
    block: suspend (stagedFile: File) -> T
): StagedOutputResult<T> {
    return withStagedOutputFiles(
        directory = directory,
        requests = listOf(
            StagedOutputRequest(
                baseName = baseName,
                extension = extension,
                block = block
            )
        )
    ).single()
}

/**
 * Stages a complete set of outputs before publishing any final file.
 *
 * Writer failure or cancellation removes every staged file without exposing a partial result
 * set. Publication is performed only after all writers succeed. If publication itself fails,
 * finals already created by this transaction are rolled back before the failure is rethrown.
 */
suspend fun <T> withStagedOutputFiles(
    directory: File,
    requests: List<StagedOutputRequest<T>>
): List<StagedOutputResult<T>> {
    require(directory.exists() || directory.mkdirs()) {
        "Unable to create output directory: ${directory.absolutePath}"
    }
    if (requests.isEmpty()) return emptyList()

    val prepared = mutableListOf<PreparedStagedOutput<T>>()
    try {
        requests.forEach { request ->
            currentCoroutineContext().ensureActive()
            val stagedFile = File.createTempFile(".anydoc_stage_", ".part", directory)
            try {
                currentCoroutineContext().ensureActive()
                val value = request.block(stagedFile)
                require(stagedFile.isFile) { "Staged output was not created." }
                prepared += PreparedStagedOutput(
                    stagedFile = stagedFile,
                    request = request,
                    value = value
                )
            } catch (t: Throwable) {
                if (stagedFile.exists()) {
                    stagedFile.delete()
                }
                throw t
            }
        }

        currentCoroutineContext().ensureActive()
        val published = mutableListOf<File>()
        try {
            val results = prepared.map { item ->
                currentCoroutineContext().ensureActive()
                val outputFile = moveStagedFileWithoutOverwrite(
                    stagedFile = item.stagedFile,
                    directory = directory,
                    baseName = item.request.baseName,
                    extension = item.request.extension
                )
                published += outputFile
                StagedOutputResult(outputFile = outputFile, value = item.value)
            }
            currentCoroutineContext().ensureActive()
            return results
        } catch (t: Throwable) {
            published.asReversed().forEach { outputFile ->
                runCatching {
                    Files.deleteIfExists(outputFile.toPath())
                }.exceptionOrNull()?.let(t::addSuppressed)
            }
            throw t
        }
    } finally {
        prepared.forEach { item ->
            if (item.stagedFile.exists()) {
                item.stagedFile.delete()
            }
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
