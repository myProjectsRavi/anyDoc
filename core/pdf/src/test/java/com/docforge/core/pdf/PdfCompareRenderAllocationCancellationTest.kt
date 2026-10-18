package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompareRenderAllocationCancellationTest {
    private class FakeBitmap {
        var recycled = 0
        fun recycle() { recycled++ }
    }

    @Test fun successfulRenderChecksBeforeAllocationAndTransfersOwnership() {
        val events = mutableListOf<String>()
        val bitmap = FakeBitmap()
        val result = renderComparePageWithPreflight(
            checkCancelled = { events += "check" },
            allocate = { events += "allocate"; bitmap },
            recycle = { events += "recycle"; it.recycle() }
        ) { target ->
            assertSame(bitmap, target)
            events += "render"
        }
        assertSame(bitmap, result)
        assertEquals(listOf("check", "allocate", "render"), events)
        assertEquals(0, bitmap.recycled)
    }

    @Test fun cancelledPreflightNeverAllocatesOrRenders() {
        val cancelled = CancellationException("cancelled before bitmap")
        var allocations = 0
        var renders = 0
        val thrown = assertThrows(CancellationException::class.java) {
            renderComparePageWithPreflight(
                checkCancelled = { throw cancelled },
                allocate = { allocations++; FakeBitmap() },
                recycle = FakeBitmap::recycle
            ) { renders++ }
        }
        assertSame(cancelled, thrown)
        assertEquals(0, allocations)
        assertEquals(0, renders)
    }

    @Test fun allocationFailurePreservesExceptionWithoutRecycling() {
        val failure = OutOfMemoryError("simulated allocation failure")
        var recycleCount = 0
        val thrown = assertThrows(OutOfMemoryError::class.java) {
            renderComparePageWithPreflight(
                checkCancelled = {},
                allocate = { throw failure },
                recycle = { _: FakeBitmap -> recycleCount++ }
            ) { }
        }
        assertSame(failure, thrown)
        assertEquals(0, recycleCount)
    }

    @Test fun renderFailureRecyclesAllocatedBitmapExactlyOnce() {
        val bitmap = FakeBitmap()
        val failure = IllegalStateException("page render failed")
        val thrown = assertThrows(IllegalStateException::class.java) {
            renderComparePageWithPreflight(
                checkCancelled = {},
                allocate = { bitmap },
                recycle = FakeBitmap::recycle
            ) { throw failure }
        }
        assertSame(failure, thrown)
        assertEquals(1, bitmap.recycled)
    }

    @Test fun recyclingFailureDoesNotReplaceRenderingFailure() {
        val bitmap = FakeBitmap()
        val primary = IllegalStateException("render")
        val cleanup = IllegalArgumentException("recycle")
        val thrown = assertThrows(IllegalStateException::class.java) {
            renderComparePageWithPreflight(
                checkCancelled = {},
                allocate = { bitmap },
                recycle = { it.recycle(); throw cleanup }
            ) { throw primary }
        }
        assertSame(primary, thrown)
        assertSame(cleanup, thrown.suppressed.single())
        assertEquals(1, bitmap.recycled)
    }

    @Test fun cancellationBetweenLeftAndRightRendersRecyclesLeftWithoutAllocatingRight() {
        val left = FakeBitmap()
        var rightAllocations = 0
        var rightRendered = false
        var ownedLeft: FakeBitmap? = null
        var ownedRight: FakeBitmap? = null
        val cancelled = CancellationException("cancelled before right render")
        var checks = 0
        val thrown = assertThrows(CancellationException::class.java) {
            withComparePageBitmapCleanup(
                resources = { listOf(ownedLeft, ownedRight) },
                recycle = FakeBitmap::recycle
            ) {
                ownedLeft = renderComparePageWithPreflight(
                    checkCancelled = { checks++ },
                    allocate = { left },
                    recycle = FakeBitmap::recycle
                ) { }
                ownedRight = renderComparePageWithPreflight(
                    checkCancelled = { checks++; throw cancelled },
                    allocate = { rightAllocations++; FakeBitmap() },
                    recycle = FakeBitmap::recycle
                ) { rightRendered = true }
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(2, checks)
        assertEquals(1, left.recycled)
        assertEquals(0, rightAllocations)
        assertTrue(!rightRendered)
    }

    @Test fun cancellationInsideRenderRecyclesAndPreservesCancellationIdentity() {
        val bitmap = FakeBitmap()
        val cancelled = CancellationException("cancelled while rendering")
        val thrown = assertThrows(CancellationException::class.java) {
            renderComparePageWithPreflight(
                checkCancelled = {},
                allocate = { bitmap },
                recycle = FakeBitmap::recycle
            ) { throw cancelled }
        }
        assertSame(cancelled, thrown)
        assertEquals(1, bitmap.recycled)
    }

    @Test fun noExtraCancellationCheckAfterSuccessfulAllocation() {
        var checks = 0
        val bitmap = FakeBitmap()
        renderComparePageWithPreflight(
            checkCancelled = { checks++ },
            allocate = { bitmap },
            recycle = FakeBitmap::recycle
        ) { }
        assertEquals(1, checks)
        assertEquals(0, bitmap.recycled)
    }
}
