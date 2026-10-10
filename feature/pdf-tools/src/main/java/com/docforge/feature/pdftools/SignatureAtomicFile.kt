package com.docforge.feature.pdftools

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Writes a complete PNG before atomically replacing the visible signature slot. */
internal fun writePngAtomically(destination: File, encode: (OutputStream) -> Boolean) {
    val directory = requireNotNull(destination.parentFile) { "Signature destination needs a parent directory." }
    require(directory.isDirectory) { "Signature directory does not exist." }

    val staged = File.createTempFile("${destination.name}.", ".tmp", directory)
    try {
        FileOutputStream(staged).use { output ->
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedIOException("Signature save interrupted before encoding.")
            }
            if (!encode(output)) throw IOException("PNG encoding failed.")
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedIOException("Signature save interrupted before syncing.")
            }
            output.fd.sync()
        }
        if (Thread.currentThread().isInterrupted) {
            throw InterruptedIOException("Signature save interrupted before publication.")
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
