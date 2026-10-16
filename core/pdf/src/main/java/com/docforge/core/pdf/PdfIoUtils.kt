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
import java.nio.file.LinkOption
import java.nio.file.Files

internal const val FILE_CHANNEL_COPY_CHUNK_BYTES = 8L * 1024L * 1024L

internal suspend fun copyChunkWithCancellation(copyChunk: () -> Long): Long {
    currentCoroutineContext().ensureActive()
    return copyChunk()
}
internal const val CACHE_COPY_FREE_SPACE_RESERVE_BYTES = 32L * 1024L * 1024L
internal const val CACHE_COPY_SPACE_MULTIPLIER = 2L
private val SAFE_EXTENSION_REGEX = Regex("[a-z0-9]{1,8}")

private fun validateOutputName(baseName: String, extension: String) {
    fun valid(part: String) = part.isNotBlank() && part != "." && part != ".." &&
        part.none { it == '/' || it == '\\' || Character.isISOControl(it) }
    require(valid(baseName) && valid(extension)) { "Invalid output name." }
}

internal suspend fun Context.copyUriToCacheFile(
    uri: Uri,
    prefix: String,
    suffix: String = guessTempSuffix(uri, defaultSuffix = ".bin"),
    keepRegistered: Boolean = false
): File {
    preflightCacheCopy(uri)
    val tempFile = File.createTempFile(prefix, suffix, cacheDir)
    ActiveTempFileRegistry.register(tempFile)
    return withRegisteredCacheCopy(tempFile, keepRegistered) {
        val input = contentResolver.openInputStream(uri) ?: error("Unable to open input: $uri")
        input.use { source ->
            Channels.newChannel(source).use { sourceChannel ->
                FileOutputStream(tempFile).channel.use { targetChannel ->
                    var position = 0L
                    while (true) {
                        require(canStartUnknownSizeCacheCopy(StatFs(cacheDir.absolutePath).availableBytes)) {
                            "Not enough temporary storage to safely continue copying this file."
                        }
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
    return withScopedCacheCopy(tempFile, block)
}

internal fun canStartUnknownSizeCacheCopy(availableBytes: Long): Boolean =
    availableBytes >= CACHE_COPY_FREE_SPACE_RESERVE_BYTES + FILE_CHANNEL_COPY_CHUNK_BYTES

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

private fun requireNonSymlinkOutputDirectory(directory: File) {
    // Examine each lexical component, not the canonical path: resolving first would
    // erase evidence that a parent redirects staging to a different directory.
    // NOFOLLOW checks are best-effort and do not make later path operations race-free.
    var component = directory.toPath().toAbsolutePath().normalize()
    while (component != null) {
        require(!Files.isSymbolicLink(component)) {
            "Output directory must not contain a symbolic-link component."
        }
        component = component.parent
    }
}

private fun File.isOccupiedOutputName(): Boolean = exists() || Files.isSymbolicLink(toPath())

/** Remove generated staging entries without traversing symbolic links or buffering a directory tree. */
private fun deleteStagedEntry(stagedFile: File) {
    val path = stagedFile.toPath()
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return
    Files.walkFileTree(path, object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
        override fun visitFile(
            file: java.nio.file.Path,
            attrs: java.nio.file.attribute.BasicFileAttributes
        ): java.nio.file.FileVisitResult {
            Files.deleteIfExists(file)
            return java.nio.file.FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: java.nio.file.Path, exc: java.io.IOException?): java.nio.file.FileVisitResult {
            if (exc != null) throw exc
            Files.deleteIfExists(dir)
            return java.nio.file.FileVisitResult.CONTINUE
        }
    })
}

/**
 * Prefer the NIO no-follow link count when available. Some Android providers
 * omit unix:nlink even though the platform lstat API can report st_nlink.
 * Neither path lookup is atomic with a later rename; unknown counts remain unknown.
 */
private fun readNioStagedHardLinkCount(file: File): Long? {
    return try {
        (Files.getAttribute(file.toPath(), "unix:nlink", LinkOption.NOFOLLOW_LINKS) as? Number)?.toLong()
    } catch (_: UnsupportedOperationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun readAndroidStagedHardLinkCount(file: File): Long? {
    return try {
        android.system.Os.lstat(file.absolutePath).st_nlink
    } catch (_: android.system.ErrnoException) {
        null
    }
}

/**
 * The injectable readers allow JVM tests to exercise the Android fallback path
 * without claiming that host unit tests execute Android's Os implementation.
 */
internal fun stagedHardLinkCount(
    file: File,
    readNioCount: (File) -> Long? = ::readNioStagedHardLinkCount,
    readAndroidCount: (File) -> Long? = ::readAndroidStagedHardLinkCount
): Long? = readNioCount(file) ?: readAndroidCount(file)

private fun requireNoKnownStagedHardLinks(file: File) {
    val count = stagedHardLinkCount(file)
    require(count == null || count == 1L) {
        "Staged output has multiple hard links."
    }
}

private fun requireRegularStagedOutput(stagedFile: File) {
    require(Files.isRegularFile(stagedFile.toPath(), LinkOption.NOFOLLOW_LINKS)) {
        "Staged output must be a regular file."
    }
    requireNoKnownStagedHardLinks(stagedFile)
}

fun resolveNonConflictingFile(directory: File, baseName: String, extension: String): File {
    validateOutputName(baseName, extension)
    requireNonSymlinkOutputDirectory(directory)
    val candidate = File(directory, "$baseName.$extension")
    if (!candidate.isOccupiedOutputName()) return candidate
    var counter = 1
    while (true) {
        val numbered = File(directory, "${baseName}_$counter.$extension")
        if (!numbered.isOccupiedOutputName()) return numbered
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

private data class PublishedStagedOutput(val outputFile: File, val fileKey: Any?)

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
    requests.forEach { validateOutputName(it.baseName, it.extension) }
    requireNonSymlinkOutputDirectory(directory)
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
                requireRegularStagedOutput(stagedFile)
                prepared += PreparedStagedOutput(
                    stagedFile = stagedFile,
                    request = request,
                    value = value
                )
            } catch (t: Throwable) {
                runCatching { deleteStagedEntry(stagedFile) }
                    .exceptionOrNull()?.let(t::addSuppressed)
                throw t
            }
        }

        currentCoroutineContext().ensureActive()
        prepared.forEach { requireRegularStagedOutput(it.stagedFile) }
        val published = mutableListOf<PublishedStagedOutput>()
        try {
            val results = prepared.map { item ->
                currentCoroutineContext().ensureActive()
                val publishedOutput = synchronized(stagedOutputPublicationLock) {
                    moveStagedFileWithoutOverwrite(
                        stagedFile = item.stagedFile,
                        directory = directory,
                        baseName = item.request.baseName,
                        extension = item.request.extension
                    )
                }
                published += publishedOutput
                StagedOutputResult(outputFile = publishedOutput.outputFile, value = item.value)
            }
            currentCoroutineContext().ensureActive()
            return results
        } catch (t: Throwable) {
            published.asReversed().forEach { publishedOutput ->
                runCatching {
                    rollbackPublishedOutputIfUnchanged(publishedOutput.outputFile, publishedOutput.fileKey)
                }.exceptionOrNull()?.let(t::addSuppressed)
            }
            throw t
        }
    } finally {
        prepared.forEach { item ->
            runCatching { deleteStagedEntry(item.stagedFile) }
        }
    }
}

/**
 * Roll back only a regular entry whose NOFOLLOW file identity still matches the entry
 * recorded at publication. Unknown file keys fail closed: leave the output rather than
 * risk deleting an unrelated replacement. This is not an atomic check-and-unlink;
 * independently mutable paths can still change between the check and deletion.
 */
internal fun rollbackPublishedOutputIfUnchanged(outputFile: File, expectedFileKey: Any?): Boolean {
    if (expectedFileKey == null) return false
    val path = outputFile.toPath()
    val attributes = try {
        Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
    } catch (_: java.nio.file.NoSuchFileException) {
        return false
    }
    if (!attributes.isRegularFile || attributes.fileKey() != expectedFileKey) return false
    return Files.deleteIfExists(path)
}

/** Same-process publication lock prevents two staged writers selecting the same destination. */
private val stagedOutputPublicationLock = Any()

/**
 * Check the published path after renaming: the staged entry can be replaced between the
 * original NOFOLLOW check and Files.move. Renaming a symlink moves the link itself; do not
 * allow it to remain as a user-visible output. Where available, fileKey also detects a
 * substituted regular file. The optional hook exercises that exact window in JVM tests.
 *
 * This narrows, but does not eliminate, races involving independently mutable filesystem
 * paths. Ancestor swaps and hard-link aliases require separate mitigations.
 */
internal fun publishCheckedStagedOutput(
    stagedFile: File,
    candidate: File,
    beforeMove: () -> Unit = {}
): Any? {
    val source = stagedFile.toPath()
    val target = candidate.toPath()
    val sourceAttributes = Files.readAttributes(
        source, java.nio.file.attribute.BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS
    )
    require(sourceAttributes.isRegularFile) { "Staged output must be a regular file." }
    requireNoKnownStagedHardLinks(stagedFile)
    beforeMove()
    // Do not use ATOMIC_MOVE here: implementations may replace existing destinations.
    Files.move(source, target)
    return try {
        val publishedAttributes = Files.readAttributes(
            target, java.nio.file.attribute.BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS
        )
        require(publishedAttributes.isRegularFile) {
            "Published output must be a regular file."
        }
        requireNoKnownStagedHardLinks(candidate)
        val sourceKey = sourceAttributes.fileKey()
        val publishedKey = publishedAttributes.fileKey()
        require(sourceKey == null || publishedKey == null || sourceKey == publishedKey) {
            "Staged output changed before publication."
        }
        publishedKey
    } catch (failure: Throwable) {
        // Only the entry just moved by this invocation is targeted; never traverse symlinks.
        runCatching { deleteStagedEntry(candidate) }
            .exceptionOrNull()?.let(failure::addSuppressed)
        throw failure
    }
}

private fun moveStagedFileWithoutOverwrite(
    stagedFile: File,
    directory: File,
    baseName: String,
    extension: String
): PublishedStagedOutput {
    var counter = 0
    while (counter < 10_000) {
        val candidate = if (counter == 0) {
            File(directory, "$baseName.$extension")
        } else {
            File(directory, "${baseName}_$counter.$extension")
        }

        if (candidate.isOccupiedOutputName()) {
            counter++
            continue
        }

        try {
            val publishedKey = publishCheckedStagedOutput(stagedFile, candidate)
            return PublishedStagedOutput(candidate, publishedKey)
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
