package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PdfOcrRasterBudgetTest {

    @Test
    fun normalA4_preservesQualityCeilingWithoutUpscaling() {
        val size = boundedPdfOcrRasterSize(
            sourceWidth = 595,
            sourceHeight = 842,
            targetLongEdge = 1800,
            maxBitmapBytes = 32L * 1024L * 1024L
        )

        assertEquals(595, size.width)
        assertEquals(842, size.height)
    }

    @Test
    fun largePage_isCappedAtQualityLongEdge() {
        val size = boundedPdfOcrRasterSize(
            sourceWidth = 4000,
            sourceHeight = 2000,
            targetLongEdge = 1800,
            maxBitmapBytes = 32L * 1024L * 1024L
        )

        assertEquals(1800, size.width)
        assertEquals(900, size.height)
    }

    @Test
    fun constrainedBudget_downscalesWithinArgbBudgetAndPreservesAspectRatio() {
        val budget = 8L * 1024L * 1024L
        val size = boundedPdfOcrRasterSize(
            sourceWidth = 4000,
            sourceHeight = 4000,
            targetLongEdge = 1800,
            maxBitmapBytes = budget
        )

        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
        assertTrue(abs(size.width.toDouble() / size.height.toDouble() - 1.0) < 0.01)
        assertTrue(size.width <= 1800)
        assertTrue(size.height <= 1800)
    }

    @Test
    fun heapAwareBudget_isClampedConservatively() {
        val mib = 1024L * 1024L

        assertEquals(8L * mib, pdfOcrBitmapBudgetBytes(32L * mib))
        assertEquals(16L * mib, pdfOcrBitmapBudgetBytes(128L * mib))
        assertEquals(32L * mib, pdfOcrBitmapBudgetBytes(256L * mib))
        assertEquals(32L * mib, pdfOcrBitmapBudgetBytes(2L * 1024L * mib))
    }

    @Test
    fun pathologicalDimensions_doNotOverflowBudgetArithmetic() {
        val budget = 8L * 1024L * 1024L
        val size = boundedPdfOcrRasterSize(
            sourceWidth = Int.MAX_VALUE,
            sourceHeight = Int.MAX_VALUE,
            targetLongEdge = 1800,
            maxBitmapBytes = budget
        )

        assertTrue(size.width > 0)
        assertTrue(size.height > 0)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test
    fun extremeAspectRatio_remainsBoundedAndPositive() {
        val budget = 8L * 1024L * 1024L
        val size = boundedPdfOcrRasterSize(
            sourceWidth = Int.MAX_VALUE,
            sourceHeight = 1,
            targetLongEdge = 1800,
            maxBitmapBytes = budget
        )

        assertTrue(size.width in 1..1800)
        assertTrue(size.height >= 1)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }
}
