package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompareOutputCoverageTest {
    @Test fun allPositiveSizeBoundaryRastersWriteEachPixelExactlyOnce() {
        val widths = listOf(1, 2, 1023, 1024, 1025, 2048, 4095, 4096, 4097, 8193)
        for (width in widths) for (height in listOf(1, 2, 3, 17)) {
            assertCompleteCoverage(width, height)
        }
    }

    @Test fun seededRandomizedPositiveRastersHaveNoGapsOrDuplicateWrites() {
        val random = Random(111)
        repeat(200) {
            assertCompleteCoverage(random.nextInt(1, 3001), random.nextInt(1, 6))
        }
    }

    @Test fun partialLastChunkOverwritesEveryPixelRegardlessOfInitialContents() {
        for (width in listOf(1, 1025, 4097, 8193)) {
            val height = 3
            val initiallyWhite = IntArray(width * height) { -1 }
            val initiallyUnknown = IntArray(width * height) { 0x12345678 }
            forEachDiffRasterChunk(width, height, {}) { row, start, count ->
                for (x in start until start + count) {
                    val value = 0xFF000000.toInt() or ((row * 31 + x) and 0xFFFFFF)
                    initiallyWhite[row * width + x] = value
                    initiallyUnknown[row * width + x] = value
                }
            }
            assertArrayEquals(initiallyWhite, initiallyUnknown)
        }
    }

    @Test fun onePixelAndNonMultipleOf1024RowsUseExactChunkRanges() {
        val onePixel = mutableListOf<Triple<Int, Int, Int>>()
        forEachDiffRasterChunk(1, 1, {}) { row, start, count ->
            onePixel += Triple(row, start, count)
        }
        assertEquals(listOf(Triple(0, 0, 1)), onePixel)

        val partial = mutableListOf<Triple<Int, Int, Int>>()
        forEachDiffRasterChunk(1025, 2, {}) { row, start, count ->
            partial += Triple(row, start, count)
        }
        assertEquals(
            listOf(Triple(0, 0, 1024), Triple(0, 1024, 1),
                Triple(1, 0, 1024), Triple(1, 1024, 1)),
            partial
        )
    }

    @Test fun cancellationBeforeFirstRowPreservesCauseAndCleansOutput() {
        val cancellation = CancellationException("stop before output")
        var cleaned = 0
        var written = 0
        val thrown = assertThrows(CancellationException::class.java) {
            withCleanupOnFailure(Unit, { cleaned++ }) {
                forEachDiffRasterChunk(1025, 2, { throw cancellation }) { _, _, count ->
                    written += count
                }
            }
        }
        assertSame(cancellation, thrown)
        assertEquals(0, written)
        assertEquals(1, cleaned)
    }

    @Test fun cancellationAt4096BoundaryPreservesCauseAndCleansPartialOutput() {
        val cancellation = CancellationException("stop at column boundary")
        var checks = 0
        var written = 0
        var cleaned = 0
        val thrown = assertThrows(CancellationException::class.java) {
            withCleanupOnFailure(Unit, { cleaned++ }) {
                forEachDiffRasterChunk(4097, 2, {
                    if (++checks == 3) throw cancellation
                }) { _, _, count -> written += count }
            }
        }
        assertSame(cancellation, thrown)
        assertEquals(3, checks) // row, column 0, column 4096
        assertEquals(4096, written)
        assertEquals(1, cleaned)
    }

    @Test fun failedChunkWritePreservesOriginalFailureAndCleansOutput() {
        val failure = IllegalStateException("pixel write failed")
        var chunks = 0
        var cleaned = 0
        val thrown = assertThrows(IllegalStateException::class.java) {
            withCleanupOnFailure(Unit, { cleaned++ }) {
                forEachDiffRasterChunk(1025, 2, {}) { _, _, _ ->
                    if (++chunks == 3) throw failure
                }
            }
        }
        assertSame(failure, thrown)
        assertEquals(3, chunks)
        assertEquals(1, cleaned)
    }

    @Test fun missingPageCopyPathDoesNotDependOnNormalRasterCoverage() {
        val present = Any()
        val copy = Any()
        val result = missingPageDiff(present, null) {
            assertSame(present, it)
            copy
        }
        assertTrue(result != null)
        assertSame(copy, result!!.first)
        assertEquals(100f, result.second, 0f)
    }

    private fun assertCompleteCoverage(width: Int, height: Int) {
        val visits = IntArray(width * height)
        forEachDiffRasterChunk(width, height, {}) { row, start, count ->
            assertTrue(row in 0 until height)
            assertTrue(start >= 0 && start + count <= width)
            assertTrue(count in 1..COMPARE_DIFF_ROW_CHUNK_PIXELS)
            for (x in start until start + count) {
                visits[row * width + x]++
            }
        }
        assertTrue("Incomplete or duplicate writes for ${width}x${height}", visits.all { it == 1 })
    }
}
