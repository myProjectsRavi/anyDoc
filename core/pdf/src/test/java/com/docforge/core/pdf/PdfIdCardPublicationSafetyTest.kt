package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test

class PdfIdCardPublicationSafetyTest {
    @Test
    fun failedWriterDoesNotPublishFinalPdf() = runBlocking {
        val directory = Files.createTempDirectory("pdf-id-card-test").toFile()
        try {
            runCatching {
                withStagedOutputFile(directory, "id_card_sheet", "pdf") { staged ->
                    staged.writeText("incomplete")
                    error("test failure")
                }
            }
            assertFalse(directory.resolve("id_card_sheet.pdf").exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
