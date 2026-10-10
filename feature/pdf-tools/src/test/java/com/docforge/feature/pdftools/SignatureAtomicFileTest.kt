package com.docforge.feature.pdftools

import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SignatureAtomicFileTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private fun slot(): File = File(temporaryFolder.root, "signature_slot_1.png")

    private fun assertNoStagingFiles() {
        assertTrue(temporaryFolder.root.listFiles().orEmpty().none { it.name.endsWith(".tmp") })
    }

    @Test fun firstSavePublishesCompletePng() {
        val destination = slot()
        writePngAtomically(destination) { it.write(byteArrayOf(1, 2, 3)); true }
        assertArrayEquals(byteArrayOf(1, 2, 3), destination.readBytes())
        assertNoStagingFiles()
    }

    @Test fun successfulReplacementPublishesNewBytes() {
        val destination = slot().apply { writeBytes(byteArrayOf(1, 2)) }
        writePngAtomically(destination) { it.write(byteArrayOf(3, 4, 5)); true }
        assertArrayEquals(byteArrayOf(3, 4, 5), destination.readBytes())
        assertNoStagingFiles()
    }

    @Test fun failedEncoderPreservesExistingSignature() {
        val destination = slot().apply { writeBytes(byteArrayOf(1, 2)) }
        assertThrows(IOException::class.java) {
            writePngAtomically(destination) { it.write(byteArrayOf(9)); false }
        }
        assertArrayEquals(byteArrayOf(1, 2), destination.readBytes())
        assertNoStagingFiles()
    }

    @Test fun thrownWritePreservesExistingSignature() {
        val destination = slot().apply { writeBytes(byteArrayOf(1, 2)) }
        assertThrows(IOException::class.java) {
            writePngAtomically(destination) {
                it.write(byteArrayOf(9))
                throw IOException("write failed")
            }
        }
        assertArrayEquals(byteArrayOf(1, 2), destination.readBytes())
        assertNoStagingFiles()
    }

    @Test fun failedFirstSaveDoesNotPublishPartialPng() {
        val destination = slot()
        assertThrows(IOException::class.java) {
            writePngAtomically(destination) { it.write(byteArrayOf(9)); false }
        }
        assertFalse(destination.exists())
        assertNoStagingFiles()
    }

    @Test fun interruptedSavePreservesExistingSignature() {
        val destination = slot().apply { writeBytes(byteArrayOf(1, 2)) }
        Thread.currentThread().interrupt()
        try {
            assertThrows(InterruptedIOException::class.java) {
                writePngAtomically(destination) { it.write(byteArrayOf(9)); true }
            }
        } finally {
            Thread.interrupted()
        }
        assertArrayEquals(byteArrayOf(1, 2), destination.readBytes())
        assertNoStagingFiles()
    }

    @Test fun interruptionDuringEncodingPreservesExistingSignature() {
        val destination = slot().apply { writeBytes(byteArrayOf(1, 2)) }
        try {
            assertThrows(InterruptedIOException::class.java) {
                writePngAtomically(destination) {
                    it.write(byteArrayOf(9))
                    Thread.currentThread().interrupt()
                    true
                }
            }
        } finally {
            Thread.interrupted()
        }
        assertArrayEquals(byteArrayOf(1, 2), destination.readBytes())
        assertNoStagingFiles()
    }
}
