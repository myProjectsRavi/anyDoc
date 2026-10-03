package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BitmapDecodeBudgetTest {

    @Test
    fun sourceBelowRequestedEdge_isNotUpscaled() {
        val size = boundedBitmapDecodeTargetSize(
            sourceWidth = 1200,
            sourceHeight = 800,
            requestedLongEdge = 2200,
            maxBitmapBytes = 16L * 1024L * 1024L
        )

        assertEquals(1200, size.width)
        assertEquals(800, size.height)
    }

    @Test
    fun thumbnailRequest_isHonoredBelowLegacy512Floor() {
        val size = boundedBitmapDecodeTargetSize(
            sourceWidth = 4000,
            sourceHeight = 3000,
            requestedLongEdge = 400,
            maxBitmapBytes = 16L * 1024L * 1024L
        )

        assertEquals(400, size.width)
        assertEquals(300, size.height)
    }

    @Test
    fun constrainedBudget_downscalesAndPreservesAspectRatio() {
        val budget = 4L * 1024L * 1024L
        val size = boundedBitmapDecodeTargetSize(
            sourceWidth = 6000,
            sourceHeight = 4000,
            requestedLongEdge = 2200,
            maxBitmapBytes = budget
        )

        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
        assertTrue(size.width <= 2200)
        assertTrue(abs(size.width.toDouble() / size.height.toDouble() - 1.5) < 0.01)
    }

    @Test
    fun heapBudget_reservesHeadroomForDecodeTransforms() {
        val mib = 1024L * 1024L

        assertEquals(4L * mib, bitmapDecodeBudgetBytes(32L * mib))
        assertEquals((128L * mib) / 12L, bitmapDecodeBudgetBytes(128L * mib))
        assertEquals(16L * mib, bitmapDecodeBudgetBytes(256L * mib))
        assertEquals(16L * mib, bitmapDecodeBudgetBytes(2L * 1024L * mib))
    }

    @Test
    fun pathologicalSquareDimensions_doNotOverflow() {
        val budget = 4L * 1024L * 1024L
        val size = boundedBitmapDecodeTargetSize(
            sourceWidth = Int.MAX_VALUE,
            sourceHeight = Int.MAX_VALUE,
            requestedLongEdge = 2200,
            maxBitmapBytes = budget
        )

        assertTrue(size.width > 0)
        assertTrue(size.height > 0)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test
    fun extremeAspectRatio_remainsPositiveAndBounded() {
        val budget = 4L * 1024L * 1024L
        val size = boundedBitmapDecodeTargetSize(
            sourceWidth = Int.MAX_VALUE,
            sourceHeight = 1,
            requestedLongEdge = 2200,
            maxBitmapBytes = budget
        )

        assertTrue(size.width in 1..2200)
        assertTrue(size.height >= 1)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test
    fun regionSampleSize_respectsQualityCeilingWithPowerOfTwoSampling() {
        val sample = boundedBitmapRegionSampleSize(
            width = 6000,
            height = 4000,
            requestedLongEdge = 2000,
            maxBitmapBytes = 16L * 1024L * 1024L
        )

        assertEquals(4, sample)
        assertTrue(6000 / sample <= 2000)
    }

    @Test
    fun regionSampleSize_becomesMoreConservativeUnderLowBudget() {
        val budget = 4L * 1024L * 1024L
        val sample = boundedBitmapRegionSampleSize(
            width = 6000,
            height = 4000,
            requestedLongEdge = 2000,
            maxBitmapBytes = budget
        )

        val decodedWidth = 6000 / sample
        val decodedHeight = 4000 / sample
        assertEquals(8, sample)
        assertTrue(decodedWidth.toLong() * decodedHeight.toLong() * 4L <= budget)
    }
}
