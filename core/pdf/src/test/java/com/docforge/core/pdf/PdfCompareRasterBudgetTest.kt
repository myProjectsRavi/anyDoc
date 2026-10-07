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

        assertEquals(12L * mib, CompareRasterBudget.heapAwareBudgetBytes(32L * mib))
        assertEquals(16L * mib, CompareRasterBudget.heapAwareBudgetBytes(128L * mib))
        assertEquals(48L * mib, CompareRasterBudget.heapAwareBudgetBytes(384L * mib))
        assertEquals(48L * mib, CompareRasterBudget.heapAwareBudgetBytes(1024L * mib))
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
