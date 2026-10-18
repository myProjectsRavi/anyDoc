package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfComparePreAllocationCancellationTest {
    @Test fun cancellationIsCheckedBeforeAllocation() {
        val events = mutableListOf<String>()
        val expected = Any()
        val result = allocateCompareDiffRaster(
            checkCancelled = { events += "check" },
            allocate = { events += "allocate"; expected }
        )
        assertEquals(listOf("check", "allocate"), events)
        assertSame(expected, result)
    }

    @Test fun cancelledOperationDoesNotAllocate() {
        val cancelled = CancellationException("cancelled before output")
        var allocations = 0
        val thrown = assertThrows(CancellationException::class.java) {
            allocateCompareDiffRaster(
                checkCancelled = { throw cancelled },
                allocate = { allocations++; Any() }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(0, allocations)
    }

    @Test fun successfulOperationAllocatesExactlyOnce() {
        var checks = 0
        var allocations = 0
        val expected = Any()
        val result = allocateCompareDiffRaster(
            checkCancelled = { checks++ },
            allocate = { allocations++; expected }
        )
        assertSame(expected, result)
        assertEquals(1, checks)
        assertEquals(1, allocations)
    }

    @Test fun allocationFailurePreservesOriginalException() {
        val failure = OutOfMemoryError("simulated bitmap allocation failure")
        var checks = 0
        val thrown = assertThrows(OutOfMemoryError::class.java) {
            allocateCompareDiffRaster(
                checkCancelled = { checks++ },
                allocate = { throw failure }
            )
        }
        assertSame(failure, thrown)
        assertEquals(1, checks)
    }

    @Test fun cancellationFailurePreservesOriginalException() {
        val failure = IllegalStateException("check failure")
        val thrown = assertThrows(IllegalStateException::class.java) {
            allocateCompareDiffRaster(
                checkCancelled = { throw failure },
                allocate = { error("must not allocate") }
            )
        }
        assertSame(failure, thrown)
    }

    @Test fun callerCleanupStillOwnsSuccessfullyAllocatedRaster() {
        val bitmap = Any()
        var cleaned = 0
        val failure = IllegalStateException("render failure")
        val thrown = assertThrows(IllegalStateException::class.java) {
            withCleanupOnFailure(
                resource = allocateCompareDiffRaster({}, { bitmap }),
                cleanup = { assertSame(bitmap, it); cleaned++ }
            ) { throw failure }
        }
        assertSame(failure, thrown)
        assertEquals(1, cleaned)
    }
}
