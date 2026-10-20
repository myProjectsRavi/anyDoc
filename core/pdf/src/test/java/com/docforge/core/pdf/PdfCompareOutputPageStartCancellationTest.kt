package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareOutputPageStartCancellationTest {
    @Test fun cancellationBeforeOutputPageStartSkipsStart() {
        val cancelled = CancellationException("before start")
        var starts = 0
        val thrown = assertThrows(CancellationException::class.java) {
            startComparePdfPageWithPreflight(
                checkCancelled = { throw cancelled },
                startPage = { starts++; Any() }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(0, starts)
    }

    @Test fun successfulOutputPageStartChecksCancellationFirst() {
        val events = mutableListOf<String>()
        val page = Any()
        val returned = startComparePdfPageWithPreflight(
            checkCancelled = { events += "check" },
            startPage = { events += "start"; page }
        )
        assertSame(page, returned)
        assertEquals(listOf("check", "start"), events)
    }

    @Test fun preflightFailurePreservesOriginalException() {
        val failure = IllegalStateException("check")
        var starts = 0
        val thrown = assertThrows(IllegalStateException::class.java) {
            startComparePdfPageWithPreflight(
                checkCancelled = { throw failure },
                startPage = { starts++; Any() }
            )
        }
        assertSame(failure, thrown)
        assertEquals(0, starts)
    }

    @Test fun pageStartFailurePreservesOriginalException() {
        val failure = IllegalArgumentException("start")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            startComparePdfPageWithPreflight(
                checkCancelled = {},
                startPage = { throw failure }
            )
        }
        assertSame(failure, thrown)
    }

    @Test fun cancellationAfterRasterWorkDoesNotStartOutputPage() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("after raster")
        var cancelledNow = false
        events += "raster-complete"
        cancelledNow = true
        val thrown = assertThrows(CancellationException::class.java) {
            startComparePdfPageWithPreflight(
                checkCancelled = {
                    events += "check"
                    if (cancelledNow) throw cancelled
                },
                startPage = { events += "start"; Any() }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("raster-complete", "check"), events)
    }

    @Test fun cancellationBetweenOutputPagesSkipsSecondStart() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("second page")
        var checks = 0
        val first = startComparePdfPageWithPreflight(
            checkCancelled = { checks++; events += "check:1" },
            startPage = { events += "start:1"; 1 }
        )
        assertEquals(1, first)
        val thrown = assertThrows(CancellationException::class.java) {
            startComparePdfPageWithPreflight(
                checkCancelled = { checks++; events += "check:2"; throw cancelled },
                startPage = { events += "start:2"; 2 }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(2, checks)
        assertEquals(listOf("check:1", "start:1", "check:2"), events)
    }
}
