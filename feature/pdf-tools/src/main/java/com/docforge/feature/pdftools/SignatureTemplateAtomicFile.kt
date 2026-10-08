package com.docforge.feature.pdftools

import java.io.File
import java.io.FileOutputStream
import java.io.InterruptedIOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Publishes a complete JSON snapshot without truncating the previous saved templates. */
internal fun writeJsonAtomically(
    destination: File,
    json: String,
    write: (OutputStream, ByteArray) -> Unit = { output, bytes -> output.write(bytes) }
) {
    val directory = requireNotNull(destination.parentFile) { "Template destination needs a parent directory." }
    require(directory.isDirectory || directory.mkdirs()) { "Cannot create template directory." }
    if (Thread.currentThread().isInterrupted) {
        throw InterruptedIOException("Template save interrupted before encoding.")
    }
    val bytes = json.toByteArray(StandardCharsets.UTF_8)
    val staged = File.createTempFile("${destination.name}.", ".tmp", directory)
    try {
        FileOutputStream(staged).use { output ->
            write(output, bytes)
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedIOException("Template save interrupted before syncing.")
            }
            output.fd.sync()
        }
        if (Thread.currentThread().isInterrupted) {
            throw InterruptedIOException("Template save interrupted before publication.")
        }
        Files.move(
            staged.toPath(),
            destination.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )
    } finally {
        staged.delete()
    }
}
