package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test

class PdfCreatorPublicationSafetyTest {
    @Test
    fun failedWriterDoesNotPublishFinalPdf() = runBlocking {
        val directory = Files.createTempDirectory("pdf-creator-test").toFile()
        try {
            runCatching {
                withStagedOutputFile(directory, "created", "pdf") { staged ->
                    staged.writeText("incomplete")
                    error("test failure")
                }
            }
            assertFalse(directory.resolve("created.pdf").exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
