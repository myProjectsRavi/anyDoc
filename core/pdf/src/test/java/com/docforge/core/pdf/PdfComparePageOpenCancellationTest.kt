package com.docforge.core.pdf

import java.io.Closeable
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfComparePageOpenCancellationTest {
    private class FakePage : Closeable {
        var closeCount = 0
        override fun close() { closeCount++ }
    }

    private class FakeBitmap {
        var recycleCount = 0
        fun recycle() { recycleCount++ }
    }

    @Test fun cancelledPreflightNeverOpensPage() {
        val cancelled = CancellationException("cancelled before open")
        var opens = 0
        val thrown = assertThrows(CancellationException::class.java) {
            openComparePageWithPreflight(
                checkCancelled = { throw cancelled },
                openPage = { opens++; FakePage() }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(0, opens)
    }

    @Test fun successfulPreflightChecksBeforeOpeningAndReturnsPage() {
        val events = mutableListOf<String>()
        val page = FakePage()
        val actual = openComparePageWithPreflight(
            checkCancelled = { events += "check" },
            openPage = { events += "open"; page }
        )
        assertSame(page, actual)
        assertEquals(listOf("check", "open"), events)
        assertEquals(0, page.closeCount)
    }

    @Test fun openFailurePreservesOriginalThrowable() {
        val failure = IllegalStateException("open failed")
        var checks = 0
        val thrown = assertThrows(IllegalStateException::class.java) {
            openComparePageWithPreflight(
                checkCancelled = { checks++ },
                openPage = { throw failure }
            )
        }
        assertSame(failure, thrown)
        assertEquals(1, checks)
    }

    @Test fun cancellationAfterOpeningClosesPageWithoutAllocatingBitmap() {
        val page = FakePage()
        val cancelled = CancellationException("cancelled before bitmap")
        var allocations = 0
        val events = mutableListOf<String>()
        val thrown = assertThrows(CancellationException::class.java) {
            openComparePageWithPreflight(
                checkCancelled = { events += "before open" },
                openPage = { events += "open"; page }
            ).use {
                renderComparePageWithPreflight(
                    checkCancelled = { events += "before bitmap"; throw cancelled },
                    allocate = { allocations++; FakeBitmap() },
                    recycle = FakeBitmap::recycle
                ) { }
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("before open", "open", "before bitmap"), events)
        assertEquals(0, allocations)
        assertEquals(1, page.closeCount)
    }

    @Test fun renderingFailureClosesPageAndRecyclesBitmap() {
        val page = FakePage()
        val bitmap = FakeBitmap()
        val failure = IllegalArgumentException("render failed")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            openComparePageWithPreflight(
                checkCancelled = {},
                openPage = { page }
            ).use {
                renderComparePageWithPreflight(
                    checkCancelled = {},
                    allocate = { bitmap },
                    recycle = FakeBitmap::recycle
                ) { throw failure }
            }
        }
        assertSame(failure, thrown)
        assertEquals(1, bitmap.recycleCount)
        assertEquals(1, page.closeCount)
    }

    @Test fun successfulRenderTransfersBitmapOwnershipAndClosesPage() {
        val page = FakePage()
        val bitmap = FakeBitmap()
        val events = mutableListOf<String>()
        val actual = openComparePageWithPreflight(
            checkCancelled = { events += "open preflight" },
            openPage = { events += "open"; page }
        ).use {
            renderComparePageWithPreflight(
                checkCancelled = { events += "bitmap preflight" },
                allocate = { events += "allocate"; bitmap },
                recycle = FakeBitmap::recycle
            ) { events += "render" }
        }
        assertSame(bitmap, actual)
        assertEquals(listOf("open preflight", "open", "bitmap preflight", "allocate", "render"), events)
        assertEquals(1, page.closeCount)
        assertEquals(0, bitmap.recycleCount)
    }

    @Test fun cancellationPreflightPreservesOriginalAndSkipsPageOpen() {
        val cancelled = CancellationException("same object")
        val page = FakePage()
        var opened = false
        val actual = assertThrows(CancellationException::class.java) {
            openComparePageWithPreflight(
                checkCancelled = { throw cancelled },
                openPage = { opened = true; page }
            )
        }
        assertSame(cancelled, actual)
        assertTrue(!opened)
        assertEquals(0, page.closeCount)
    }
}
