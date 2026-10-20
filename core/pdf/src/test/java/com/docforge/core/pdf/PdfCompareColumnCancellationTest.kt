package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareColumnCancellationTest {
    @Test fun smallRowVisitsEveryPixelWithOneCancellationCheck() {
        val visited = mutableListOf<Int>()
        var checks = 0
        forEachDiffColumn(5, { checks++ }) { visited += it }
        assertEquals(listOf(0, 1, 2, 3, 4), visited)
        assertEquals(1, checks)
    }

    @Test fun exactBoundaryDoesNotTriggerPrematureExtraCheck() {
        var checks = 0
        var pixels = 0
        forEachDiffColumn(4096, { checks++ }) { pixels++ }
        assertEquals(4096, pixels)
        assertEquals(1, checks)
    }

    @Test fun checksAgainAtEach4096PixelBoundary() {
        val checkedAfter = mutableListOf<Int>()
        var visited = 0
        forEachDiffColumn(8193, { checkedAfter += visited }) { visited++ }
        assertEquals(listOf(0, 4096, 8192), checkedAfter)
        assertEquals(8193, visited)
    }

    @Test fun emptyRowDoesNotCheckOrProcessPixels() {
        var checks = 0
        var pixels = 0
        forEachDiffColumn(0, { checks++ }) { pixels++ }
        assertEquals(0, checks)
        assertEquals(0, pixels)
    }

    @Test fun cancellationBeforeFirstPixelStopsImmediately() {
        val cancelled = CancellationException("cancel")
        var pixels = 0
        val thrown = assertThrows(CancellationException::class.java) {
            forEachDiffColumn(5000, { throw cancelled }) { pixels++ }
        }
        assertSame(cancelled, thrown)
        assertEquals(0, pixels)
    }

    @Test fun cancellationAtNextCheckpointStopsWideRow() {
        val cancelled = CancellationException("cancel")
        var checks = 0
        var pixels = 0
        val thrown = assertThrows(CancellationException::class.java) {
            forEachDiffColumn(10000, {
                checks++
                if (checks == 2) throw cancelled
            }) { pixels++ }
        }
        assertSame(cancelled, thrown)
        assertEquals(2, checks)
        assertEquals(4096, pixels)
    }
}
