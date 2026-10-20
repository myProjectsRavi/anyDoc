package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfComparePageGeometryTest {
    private val portrait = CompareRasterSize(595, 842)
    private val landscape = CompareRasterSize(842, 595)

    @Test
    fun equalPagesPreserveOriginalPdfPointDimensions() {
        assertEquals(portrait, compareOutputPageSize(portrait, portrait))
        assertEquals(CompareRasterSize(892, 1263), scaledCompareRasterSize(portrait, 1.5f))
        assertEquals(portrait, compareOutputPageSize(portrait, portrait))
    }

    @Test
    fun unequalPagesUseMaximumSourceWidthAndHeight() {
        assertEquals(CompareRasterSize(842, 842), compareOutputPageSize(portrait, landscape))
        assertEquals(CompareRasterSize(842, 842), compareOutputPageSize(landscape, portrait))
    }

    @Test
    fun leftMissingPageUsesRightSourceGeometry() {
        assertEquals(portrait, compareOutputPageSize(null, portrait))
    }

    @Test
    fun rightMissingPageUsesLeftSourceGeometry() {
        assertEquals(landscape, compareOutputPageSize(landscape, null))
    }

    @Test
    fun outputGeometryDoesNotDependOnRasterScaleOrMemoryFit() {
        val smallRaster = scaledCompareRasterSize(portrait, 0.5f)
        val largeRaster = scaledCompareRasterSize(portrait, 4f)
        assertEquals(CompareRasterSize(297, 421), smallRaster)
        assertEquals(CompareRasterSize(2380, 3368), largeRaster)
        assertEquals(portrait, compareOutputPageSize(portrait, null))
    }

    @Test
    fun bothMissingPagesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            compareOutputPageSize(null, null)
        }
    }
}
