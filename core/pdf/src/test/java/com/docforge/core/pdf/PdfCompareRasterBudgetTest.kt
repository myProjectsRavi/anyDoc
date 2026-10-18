package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompareRasterBudgetTest {

    @Test
    fun combinedWorkingSetCountsBothInputsAndLargestDiffCanvas() {
        val left = CompareRasterSize(2, 3)
        val right = CompareRasterSize(4, 5)

        // 6 left pixels + 20 right pixels + 20 diff pixels, all ARGB_8888.
        assertEquals(184L, CompareRasterBudget.workingSetBytes(left, right))
        assertEquals(left to right, CompareRasterBudget.fit(left, right, 184L))
    }

    @Test
    fun missingPageStillBudgetsInputAndCopiedDiffBitmap() {
        val right = CompareRasterSize(4, 5)

        assertEquals(160L, CompareRasterBudget.workingSetBytes(null, right))
        assertEquals(null to right, CompareRasterBudget.fit(null, right, 160L))
    }

    @Test
    fun heapBudgetIsClampedToConservativeRange() {
        val mib = 1024L * 1024L

        assertEquals(2L * mib, CompareRasterBudget.heapAwareBudgetBytes(16L * mib))
        assertEquals(4L * mib, CompareRasterBudget.heapAwareBudgetBytes(32L * mib))
        assertEquals(8L * mib, CompareRasterBudget.heapAwareBudgetBytes(64L * mib))
        assertEquals(12L * mib, CompareRasterBudget.heapAwareBudgetBytes(96L * mib))
        assertEquals(16L * mib, CompareRasterBudget.heapAwareBudgetBytes(128L * mib))
        assertEquals(48L * mib, CompareRasterBudget.heapAwareBudgetBytes(384L * mib))
        assertEquals(48L * mib, CompareRasterBudget.heapAwareBudgetBytes(1024L * mib))
    }

    @Test
    fun occupiedHeapReducesRasterBudgetWithoutChangingEmptyHeapBudget() {
        val mib = 1024L * 1024L
        assertEquals(16L * mib, CompareRasterBudget.heapAwareBudgetBytes(128L * mib))
        assertEquals(1L * mib, CompareRasterBudget.heapAwareBudgetBytes(128L * mib, 120L * mib))
        assertEquals(2L * mib, CompareRasterBudget.heapAwareBudgetBytes(128L * mib, 112L * mib))
        assertEquals(12L, CompareRasterBudget.heapAwareBudgetBytes(128L * mib, 128L * mib))
    }

    @Test
    fun inconsistentHeapOccupancyCannotOverflowRemainingBudget() {
        val mib = 1024L * 1024L
        assertEquals(4L * mib, CompareRasterBudget.heapAwareBudgetBytes(32L * mib, -1L))
        assertEquals(12L, CompareRasterBudget.heapAwareBudgetBytes(32L * mib, 100L * mib))
        assertEquals(12L, CompareRasterBudget.heapAwareBudgetBytes(0L, Long.MAX_VALUE))
        assertEquals(48L * mib, CompareRasterBudget.heapAwareBudgetBytes(Long.MAX_VALUE, Long.MIN_VALUE))
    }

    @Test
    fun occupiedHeapBudgetFitsCombinedThreeRasterWorkingSet() {
        val mib = 1024L * 1024L
        val budget = CompareRasterBudget.heapAwareBudgetBytes(128L * mib, 120L * mib)
        val requested = CompareRasterSize(16000, 1000)
        val fitted = CompareRasterBudget.fit(requested, requested, budget)
        assertTrue(CompareRasterBudget.workingSetBytes(fitted.first, fitted.second) <= budget)
    }

    @Test
    fun smallHeapBudgetFitsCombinedLeftRightAndDiffRasters() {
        val mib = 1024L * 1024L
        val left = CompareRasterSize(16000, 1000)
        val right = CompareRasterSize(1000, 16000)

        for (heapMib in listOf(16L, 32L, 64L)) {
            val maxHeap = heapMib * mib
            val budget = CompareRasterBudget.heapAwareBudgetBytes(maxHeap)
            assertTrue("Budget must not exceed one eighth of a small heap", budget <= maxHeap / 8L)

            val (fittedLeft, fittedRight) = CompareRasterBudget.fit(left, right, budget)
            assertTrue(
                "Combined left, right, and diff bitmaps must fit the budget",
                CompareRasterBudget.workingSetBytes(fittedLeft, fittedRight) <= budget
            )
            assertTrue(fittedLeft!!.width in 1..left.width)
            assertTrue(fittedRight!!.height in 1..right.height)
        }
    }

    @Test
    fun minimumRasterBudgetSupportsTwoOnePixelInputsAndDiff() {
        val pixel = CompareRasterSize(1, 1)
        val minimum = CompareRasterBudget.heapAwareBudgetBytes(0L)

        assertEquals(12L, minimum)
        assertEquals(12L, CompareRasterBudget.workingSetBytes(pixel, pixel))
        assertEquals(pixel to pixel, CompareRasterBudget.fit(pixel, pixel, minimum))
    }

    @Test
    fun nonPositiveDimensionsAreRejectedBeforeRasterArithmetic() {
        for (invalid in listOf(
            CompareRasterSize(0, 10),
            CompareRasterSize(10, 0),
            CompareRasterSize(-1, 10),
            CompareRasterSize(10, -1)
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                CompareRasterBudget.workingSetBytes(invalid, null)
            }
            assertThrows(IllegalArgumentException::class.java) {
                CompareRasterBudget.fit(null, invalid, 12L * 1024L * 1024L)
            }
        }
    }

    @Test
    fun constrainedBudgetFitsThreeRasterWorkingSet() {
        val budget = 12L * 1024L * 1024L
        val left = CompareRasterSize(16000, 1000)
        val right = CompareRasterSize(1000, 16000)

        val fitted = CompareRasterBudget.fit(left, right, budget)

        assertTrue(CompareRasterBudget.workingSetBytes(fitted.first, fitted.second) <= budget)
        assertTrue(fitted.first!!.width < left.width)
        assertTrue(fitted.second!!.height < right.height)
    }

    @Test
    fun pathologicalDimensionsSaturateAndFitSafely() {
        val extreme = CompareRasterSize(Int.MAX_VALUE, Int.MAX_VALUE)
        val budget = 12L * 1024L * 1024L

        assertEquals(Long.MAX_VALUE, CompareRasterBudget.workingSetBytes(extreme, extreme))
        val fitted = CompareRasterBudget.fit(extreme, extreme, budget)
        assertTrue(CompareRasterBudget.workingSetBytes(fitted.first, fitted.second) <= budget)
        assertTrue(fitted.first!!.width > 0)
        assertTrue(fitted.first!!.height > 0)
    }

    @Test
    fun impossibleMinimumWorkingSetFailsBeforeAllocation() {
        assertThrows(IllegalArgumentException::class.java) {
            CompareRasterBudget.fit(
                CompareRasterSize(1, 1),
                CompareRasterSize(1, 1),
                8L
            )
        }
    }
}
