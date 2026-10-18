package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfComparePublicationSafetyTest {

    @Test
    fun compareOutput_writerFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-compare-output").toFile()
        try {
            try {
                withStagedOutputFile(
                    directory = directory,
                    baseName = "comparison",
                    extension = "pdf"
                ) { staged ->
                    staged.writeText("partial compare pdf")
                    error("synthetic compare write failure")
                }
                fail("Expected compare writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("comparison.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun compareOutput_cancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-compare-cancel").toFile()
        try {
            try {
                withStagedOutputFile(
                    directory = directory,
                    baseName = "comparison",
                    extension = "pdf"
                ) { staged ->
                    staged.writeText("partial compare pdf")
                    throw CancellationException("synthetic cancellation")
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("comparison.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun compareRasterBudget_preservesOrdinaryRequestedDimensions() {
        val left = CompareRasterSize(900, 1200)
        val right = CompareRasterSize(900, 1200)
        val budget = 48L * 1024L * 1024L
        val fitted = CompareRasterBudget.fit(left, right, budget)
        assertEquals(left, fitted.first)
        assertEquals(right, fitted.second)
        assertTrue(CompareRasterBudget.workingSetBytes(fitted.first, fitted.second) <= budget)
    }

    @Test
    fun compareRasterBudget_boundsCombinedOpposingAspectRasters() {
        val left = CompareRasterSize(16000, 1000)
        val right = CompareRasterSize(1000, 16000)
        val budget = 12L * 1024L * 1024L
        val fitted = CompareRasterBudget.fit(left, right, budget)
        assertTrue(CompareRasterBudget.workingSetBytes(fitted.first, fitted.second) <= budget)
        assertTrue(fitted.first!!.width < left.width)
        assertTrue(fitted.second!!.height < right.height)
    }

    @Test
    fun compareRasterBudget_missingPageAccountsForDiffCopy() {
        val right = CompareRasterSize(5000, 5000)
        val budget = 12L * 1024L * 1024L
        val fitted = CompareRasterBudget.fit(null, right, budget)
        assertEquals(null, fitted.first)
        assertTrue(CompareRasterBudget.workingSetBytes(fitted.first, fitted.second) <= budget)
        assertTrue(fitted.second!!.width < right.width)
    }

    @Test
    fun compareRasterBudget_extremeDimensionsSaturateWorkingSet() {
        val extreme = CompareRasterSize(Int.MAX_VALUE, Int.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, CompareRasterBudget.workingSetBytes(extreme, extreme))
    }

    @Test
    fun compareRasterBudget_extremeDimensionsFitWithinBudget() {
        val extreme = CompareRasterSize(Int.MAX_VALUE, Int.MAX_VALUE)
        val budget = 12L * 1024L * 1024L

        val fitted = CompareRasterBudget.fit(extreme, extreme, budget)

        assertTrue(CompareRasterBudget.workingSetBytes(fitted.first, fitted.second) <= budget)
        assertTrue(fitted.first!!.width in 1 until extreme.width)
        assertTrue(fitted.first!!.height in 1 until extreme.height)
        assertEquals(fitted.first, fitted.second)
    }

}
