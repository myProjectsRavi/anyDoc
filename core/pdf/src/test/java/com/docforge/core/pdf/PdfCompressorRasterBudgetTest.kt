package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PdfCompressorRasterBudgetTest {

    @Test
    fun standardA4At150Dpi_isNotDownscaledUnderNormalBudget() {
        val size = boundedPdfRasterSize(
            pageWidthPoints = 595,
            pageHeightPoints = 842,
            renderDpi = 150,
            maxBitmapBytes = 32L * 1024L * 1024L
        )

        assertEquals(1240, size.width)
        assertEquals(1754, size.height)
    }

    @Test
    fun oversizedPage_isDownscaledWithinArgbBudget_andPreservesAspectRatio() {
        val budget = 32L * 1024L * 1024L
        val size = boundedPdfRasterSize(
            pageWidthPoints = 10_000,
            pageHeightPoints = 5_000,
            renderDpi = 150,
            maxBitmapBytes = budget
        )

        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
        assertTrue(abs(size.width.toDouble() / size.height.toDouble() - 2.0) < 0.01)
    }

    @Test
    fun heapAwareBudget_isBoundedForSmallAndLargeHeaps() {
        val mib = 1024L * 1024L

        assertEquals(8L * mib, pdfCompressionBitmapBudgetBytes(32L * mib))
        assertEquals(16L * mib, pdfCompressionBitmapBudgetBytes(128L * mib))
        assertEquals(32L * mib, pdfCompressionBitmapBudgetBytes(256L * mib))
        assertEquals(32L * mib, pdfCompressionBitmapBudgetBytes(2L * 1024L * mib))
    }

    @Test
    fun extremeAspectRatio_isBoundedWithoutIterativePixelWalkdown() {
        val budget = 8L * 1024L * 1024L
        val size = boundedPdfRasterSize(
            pageWidthPoints = Int.MAX_VALUE,
            pageHeightPoints = 1,
            renderDpi = 150,
            maxBitmapBytes = budget
        )

        assertTrue(size.width > 0)
        assertTrue(size.height > 0)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test
    fun pathologicalPageDimensions_doNotOverflowBudgetArithmetic() {
        val budget = 8L * 1024L * 1024L
        val size = boundedPdfRasterSize(
            pageWidthPoints = Int.MAX_VALUE,
            pageHeightPoints = Int.MAX_VALUE,
            renderDpi = 150,
            maxBitmapBytes = budget
        )

        assertTrue(size.width > 0)
        assertTrue(size.height > 0)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }
}
