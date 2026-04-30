package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
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
