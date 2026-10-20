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

class PdfPageImagePublicationSafetyTest {

    @Test
    fun pageImageSet_failureOnLaterPage_publishesNoPageFinals() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-page-image-set").toFile()
        try {
            try {
                withStagedOutputFiles(
                    directory = directory,
                    requests = listOf(
                        StagedOutputRequest(
                            baseName = "pages_p1",
                            extension = "jpg"
                        ) { staged ->
                            staged.writeBytes(byteArrayOf(1, 2, 3))
                            Unit
                        },
                        StagedOutputRequest(
                            baseName = "pages_p2",
                            extension = "jpg"
                        ) { staged ->
                            assertFalse(directory.resolve("pages_p1.jpg").exists())
                            staged.writeBytes(byteArrayOf(4, 5, 6))
                            error("synthetic second-page encode failure")
                        }
                    )
                )
                fail("Expected later page encoding failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("pages_p1.jpg").exists())
            assertFalse(directory.resolve("pages_p2.jpg").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun stagedZip_entryFailure_publishesNoPartialBundle() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-page-image-zip").toFile()
        try {
            try {
                withStagedOutputFile(
                    directory = directory,
                    baseName = "pages_jpg_bundle",
                    extension = "zip"
                ) { staged ->
                    ZipOutputStream(FileOutputStream(staged)).use { zipOut ->
                        zipOut.putNextEntry(ZipEntry("pages_p1.jpg"))
                        zipOut.write(byteArrayOf(1, 2, 3))
                        zipOut.closeEntry()

                        zipOut.putNextEntry(ZipEntry("pages_p2.jpg"))
                        zipOut.write(byteArrayOf(4, 5, 6))
                        error("synthetic zip entry failure")
                    }
                }
                fail("Expected ZIP entry failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("pages_jpg_bundle.zip").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
