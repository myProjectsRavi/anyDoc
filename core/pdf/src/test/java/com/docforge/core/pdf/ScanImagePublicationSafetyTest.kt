package com.docforge.core.pdf

import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScanImagePublicationSafetyTest {

    @Test
    fun scanPageSet_failureOnLaterPage_publishesNoPageFinals() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-scan-image-set").toFile()
        try {
            try {
                withStagedOutputFiles(
                    directory = directory,
                    requests = listOf(
                        StagedOutputRequest(
                            baseName = "scan_p1",
                            extension = "jpg"
                        ) { staged ->
                            staged.writeBytes(byteArrayOf(1, 2, 3))
                            Unit
                        },
                        StagedOutputRequest(
                            baseName = "scan_p2",
                            extension = "jpg"
                        ) { staged ->
                            assertFalse(directory.resolve("scan_p1.jpg").exists())
                            staged.writeBytes(byteArrayOf(4, 5, 6))
                            error("synthetic later scan-page encode failure")
                        }
                    )
                )
                fail("Expected later scan page failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("scan_p1.jpg").exists())
            assertFalse(directory.resolve("scan_p2.jpg").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun stagedScanZip_entryFailure_publishesNoPartialBundle() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-scan-image-zip").toFile()
        try {
            try {
                withStagedOutputFile(
                    directory = directory,
                    baseName = "scan_jpg_bundle",
                    extension = "zip"
                ) { staged ->
                    ZipOutputStream(FileOutputStream(staged)).use { zipOut ->
                        zipOut.putNextEntry(ZipEntry("scan_p1.jpg"))
                        zipOut.write(byteArrayOf(1, 2, 3))
                        zipOut.closeEntry()

                        zipOut.putNextEntry(ZipEntry("scan_p2.jpg"))
                        zipOut.write(byteArrayOf(4, 5, 6))
                        error("synthetic scan ZIP failure")
                    }
                }
                fail("Expected scan ZIP failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("scan_jpg_bundle.zip").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
