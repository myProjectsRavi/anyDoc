package com.docforge.feature.pdftools

import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SignatureTemplateAtomicFileTest {
    @get:Rule val folder = TemporaryFolder()

    private fun destination(): File = File(folder.root, "pdf_sign_placement_templates.json")

    private fun assertNoStages() {
        assertTrue(folder.root.listFiles().orEmpty().none { it.name.endsWith(".tmp") })
    }

    @Test fun firstSavePublishesCompleteJson() {
        val file = destination()
        writeJsonAtomically(file, "{\"templates\":[]}")
        assertEquals("{\"templates\":[]}", file.readText())
        assertNoStages()
    }

    @Test fun replacementPublishesCompleteJson() {
        val file = destination().apply { writeText("old") }
        writeJsonAtomically(file, "new")
        assertEquals("new", file.readText())
        assertNoStages()
    }

    @Test fun writeFailurePreservesPreviousJson() {
        val file = destination().apply { writeText("old") }
        assertThrows(IOException::class.java) {
            writeJsonAtomically(file, "new") { output, bytes ->
                output.write(bytes)
                throw IOException("simulated write failure")
            }
        }
        assertEquals("old", file.readText())
        assertNoStages()
    }

    @Test fun failedFirstWriteDoesNotPublishPartialJson() {
        val file = destination()
        assertThrows(IOException::class.java) {
            writeJsonAtomically(file, "new") { output, bytes ->
                output.write(bytes)
                throw IOException("simulated write failure")
            }
        }
        assertFalse(file.exists())
        assertNoStages()
    }

    @Test fun interruptedWritePreservesPreviousJson() {
        val file = destination().apply { writeText("old") }
        try {
            assertThrows(InterruptedIOException::class.java) {
                writeJsonAtomically(file, "new") { output, bytes ->
                    output.write(bytes)
                    Thread.currentThread().interrupt()
                }
            }
        } finally {
            Thread.interrupted()
        }
        assertEquals("old", file.readText())
        assertNoStages()
    }

    @Test fun failedPublicationCleansStage() {
        val directory = destination().apply { mkdirs() }
        File(directory, "existing").writeText("preserve")
        assertThrows(IOException::class.java) { writeJsonAtomically(directory, "new") }
        assertEquals("preserve", File(directory, "existing").readText())
        assertNoStages()
    }
}
