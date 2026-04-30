package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import java.io.File

private const val COPY_BUFFER_SIZE_BYTES = 8 * 1024
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
            tempFile.outputStream().use { target ->
                source.copyTo(target, COPY_BUFFER_SIZE_BYTES)
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
