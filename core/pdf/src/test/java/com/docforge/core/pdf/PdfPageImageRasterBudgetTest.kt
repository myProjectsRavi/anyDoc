package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PdfPageImageRasterBudgetTest {

    @Test
    fun normalA4AtDefaultScale_preservesRequestedDimensions() {
        val size = boundedPdfPageImageRasterSize(
            pageWidthPoints = 595,
            pageHeightPoints = 842,
            scaleFactor = 2.5f,
            maxBitmapBytes = 32L * 1024L * 1024L
        )

        assertEquals(1487, size.width)
        assertEquals(2105, size.height)
    }

    @Test
    fun oversizedPage_isDownscaledWithinArgbBudget_andPreservesAspectRatio() {
        val budget = 16L * 1024L * 1024L
        val size = boundedPdfPageImageRasterSize(
            pageWidthPoints = 12_000,
            pageHeightPoints = 6_000,
            scaleFactor = 4f,
            maxBitmapBytes = budget
        )

        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
        assertTrue(abs(size.width.toDouble() / size.height.toDouble() - 2.0) < 0.01)
    }

    @Test
    fun heapAwareBudget_isClampedConservatively() {
        val mib = 1024L * 1024L

        assertEquals(8L * mib, pdfPageImageBitmapBudgetBytes(32L * mib))
        assertEquals(16L * mib, pdfPageImageBitmapBudgetBytes(128L * mib))
        assertEquals(32L * mib, pdfPageImageBitmapBudgetBytes(256L * mib))
        assertEquals(32L * mib, pdfPageImageBitmapBudgetBytes(2L * 1024L * mib))
    }

    @Test
    fun pathologicalDimensions_doNotOverflowBudgetArithmetic() {
        val budget = 8L * 1024L * 1024L
        val size = boundedPdfPageImageRasterSize(
            pageWidthPoints = Int.MAX_VALUE,
            pageHeightPoints = Int.MAX_VALUE,
            scaleFactor = Float.MAX_VALUE,
            maxBitmapBytes = budget
        )

        assertTrue(size.width > 0)
        assertTrue(size.height > 0)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test
    fun extremeAspectRatio_isBoundedWithoutIterativePixelWalkdown() {
        val budget = 8L * 1024L * 1024L
        val size = boundedPdfPageImageRasterSize(
            pageWidthPoints = Int.MAX_VALUE,
            pageHeightPoints = 1,
            scaleFactor = 2.5f,
            maxBitmapBytes = budget
        )

        assertTrue(size.width > 0)
        assertTrue(size.height > 0)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test
    fun invalidScaleFactors_areRejectedBeforeBitmapAllocation() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedPdfPageImageRasterSize(595, 842, Float.NaN, 8L * 1024L * 1024L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            boundedPdfPageImageRasterSize(595, 842, 0f, 8L * 1024L * 1024L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            boundedPdfPageImageRasterSize(595, 842, Float.POSITIVE_INFINITY, 8L * 1024L * 1024L)
        }
    }
}
