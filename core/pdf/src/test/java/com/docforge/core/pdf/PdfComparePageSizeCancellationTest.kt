package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfComparePageSizeCancellationTest {
    private class FakePage(
        val width: Int = 595,
        val height: Int = 842,
        private val closeFailure: Throwable? = null
    ) : AutoCloseable {
        var closeCount = 0
        override fun close() {
            closeCount++
            closeFailure?.let { throw it }
        }
    }

    @Test fun cancellationBeforeDimensionOpenSkipsOpenAndRead() {
        val cancelled = CancellationException("before size")
        var opens = 0
        var reads = 0
        val thrown = assertThrows(CancellationException::class.java) {
            readComparePageSizeWithPreflight(
                checkCancelled = { throw cancelled },
                openPage = { opens++; FakePage() },
                readDimensions = { reads++; CompareRasterSize(it.width, it.height) }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(0, opens)
        assertEquals(0, reads)
    }

    @Test fun successfulDimensionReadChecksOpensReadsAndClosesInOrder() {
        val events = mutableListOf<String>()
        val page = FakePage()
        val size = readComparePageSizeWithPreflight(
            checkCancelled = { events += "check" },
            openPage = { events += "open"; page },
            readDimensions = { events += "read"; CompareRasterSize(it.width, it.height) }
        )
        events += "closed"
        assertEquals(CompareRasterSize(595, 842), size)
        assertEquals(listOf("check", "open", "read", "closed"), events)
        assertEquals(1, page.closeCount)
    }

    @Test fun pageOpenFailurePreservesOriginalException() {
        val failure = IllegalStateException("page open")
        var reads = 0
        val thrown = assertThrows(IllegalStateException::class.java) {
            readComparePageSizeWithPreflight(
                checkCancelled = {},
                openPage = { throw failure },
                readDimensions = { reads++; CompareRasterSize(1, 1) }
            )
        }
        assertSame(failure, thrown)
        assertEquals(0, reads)
    }

    @Test fun dimensionReadFailureClosesPageAndPreservesOriginal() {
        val page = FakePage()
        val failure = IllegalArgumentException("dimension read")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            readComparePageSizeWithPreflight(
                checkCancelled = {},
                openPage = { page },
                readDimensions = { throw failure }
            )
        }
        assertSame(failure, thrown)
        assertEquals(1, page.closeCount)
    }

    @Test fun closeFailureAfterSuccessfulReadIsPropagated() {
        val failure = IllegalStateException("page close")
        val page = FakePage(closeFailure = failure)
        val thrown = assertThrows(IllegalStateException::class.java) {
            readComparePageSizeWithPreflight(
                checkCancelled = {},
                openPage = { page },
                readDimensions = { CompareRasterSize(it.width, it.height) }
            )
        }
        assertSame(failure, thrown)
        assertEquals(1, page.closeCount)
    }

    @Test fun closeFailureDuringDimensionFailureIsSuppressed() {
        val failure = IllegalArgumentException("read failed")
        val cleanup = IllegalStateException("close failed")
        val page = FakePage(closeFailure = cleanup)
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            readComparePageSizeWithPreflight(
                checkCancelled = {},
                openPage = { page },
                readDimensions = { throw failure }
            )
        }
        assertSame(failure, thrown)
        assertSame(cleanup, thrown.suppressed.single())
        assertEquals(1, page.closeCount)
    }

    @Test fun cancellationBetweenLeftAndRightDimensionReadsDoesNotOpenRight() {
        val left = FakePage(612, 792)
        val right = FakePage(595, 842)
        val cancelled = CancellationException("before right size")
        var checks = 0
        var rightOpens = 0
        val leftSize = readComparePageSizeWithPreflight(
            checkCancelled = { checks++ },
            openPage = { left },
            readDimensions = { CompareRasterSize(it.width, it.height) }
        )
        assertEquals(CompareRasterSize(612, 792), leftSize)
        val thrown = assertThrows(CancellationException::class.java) {
            readComparePageSizeWithPreflight(
                checkCancelled = { checks++; throw cancelled },
                openPage = { rightOpens++; right },
                readDimensions = { CompareRasterSize(it.width, it.height) }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(2, checks)
        assertEquals(1, left.closeCount)
        assertEquals(0, rightOpens)
        assertEquals(0, right.closeCount)
    }

    @Test fun cancellationFromDimensionReadClosesPage() {
        val cancelled = CancellationException("while reading dimensions")
        val page = FakePage()
        val thrown = assertThrows(CancellationException::class.java) {
            readComparePageSizeWithPreflight(
                checkCancelled = {},
                openPage = { page },
                readDimensions = { throw cancelled }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(1, page.closeCount)
    }
}
