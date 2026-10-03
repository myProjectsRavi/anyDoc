package com.docforge.feature.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ImageFormatScaleBudgetTest {

    @Test
    fun oneToOneScale_preservesSourceDimensionsWhenWithinBudget() {
        val size = boundedScaledBitmapSize(
            sourceWidth = 1200,
            sourceHeight = 800,
            scaleFactor = 1f,
            maxBitmapBytes = 16L * 1024L * 1024L
        )

        assertEquals(1200, size.width)
        assertEquals(800, size.height)
    }

    @Test
    fun downscale_preservesRequestedScaleWhenWithinBudget() {
        val size = boundedScaledBitmapSize(
            sourceWidth = 2000,
            sourceHeight = 1000,
            scaleFactor = 0.5f,
            maxBitmapBytes = 16L * 1024L * 1024L
        )

        assertEquals(1000, size.width)
        assertEquals(500, size.height)
    }

    @Test
    fun safeUpscale_preservesRequestedScale() {
        val size = boundedScaledBitmapSize(
            sourceWidth = 800,
            sourceHeight = 600,
            scaleFactor = 2f,
            maxBitmapBytes = 16L * 1024L * 1024L
        )

        assertEquals(1600, size.width)
        assertEquals(1200, size.height)
    }

    @Test
    fun largeUpscale_isReducedToBudgetWhilePreservingAspectRatio() {
        val budget = 16L * 1024L * 1024L
        val size = boundedScaledBitmapSize(
            sourceWidth = 2048,
            sourceHeight = 2048,
            scaleFactor = 3f,
            maxBitmapBytes = budget
        )

        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
        assertTrue(size.width < 6144)
        assertTrue(size.height < 6144)
        assertTrue(abs(size.width.toDouble() / size.height.toDouble() - 1.0) < 0.01)
    }

    @Test
    fun constrainedBudget_keepsScaledAllocationWithinLimit() {
        val budget = 4L * 1024L * 1024L
        val size = boundedScaledBitmapSize(
            sourceWidth = 1600,
            sourceHeight = 900,
            scaleFactor = 3f,
            maxBitmapBytes = budget
        )

        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
        assertTrue(abs(size.width.toDouble() / size.height.toDouble() - (16.0 / 9.0)) < 0.02)
    }

    @Test
    fun pathologicalDimensions_doNotOverflow() {
        val budget = 4L * 1024L * 1024L
        val size = boundedScaledBitmapSize(
            sourceWidth = Int.MAX_VALUE,
            sourceHeight = Int.MAX_VALUE,
            scaleFactor = 3f,
            maxBitmapBytes = budget
        )

        assertTrue(size.width > 0)
        assertTrue(size.height > 0)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }
}
