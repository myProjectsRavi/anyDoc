package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompareDocumentLifecycleTest {
    private class FakeDocument(private val closeFailure: Throwable? = null) {
        var closes = 0
        fun close() {
            closes++
            closeFailure?.let { throw it }
        }
    }

    @Test fun successClosesOnceAndReturnsResult() {
        val document = FakeDocument()
        val result = withClosePreservingPrimaryFailure(document, FakeDocument::close) { "written" }
        assertEquals("written", result)
        assertEquals(1, document.closes)
    }

    @Test fun drawFailureRemainsPrimaryWhenUnfinishedPageCloseThrows() {
        val drawFailure = IllegalArgumentException("draw failed")
        val closeFailure = IllegalStateException("Current page not finished!")
        val document = FakeDocument(closeFailure)
        val actual = assertThrows(IllegalArgumentException::class.java) {
            withClosePreservingPrimaryFailure(document, FakeDocument::close) {
                throw drawFailure
            }
        }
        assertSame(drawFailure, actual)
        assertEquals(1, actual.suppressed.size)
        assertSame(closeFailure, actual.suppressed[0])
        assertEquals(1, document.closes)
    }

    @Test fun cancellationRemainsPrimaryWhenCloseThrows() {
        val cancellation = CancellationException("cancelled")
        val closeFailure = IllegalStateException("Current page not finished!")
        val document = FakeDocument(closeFailure)
        val actual = assertThrows(CancellationException::class.java) {
            withClosePreservingPrimaryFailure(document, FakeDocument::close) {
                throw cancellation
            }
        }
        assertSame(cancellation, actual)
        assertSame(closeFailure, actual.suppressed.single())
        assertEquals(1, document.closes)
    }

    @Test fun finishFailureRemainsPrimaryWhenCloseThrows() {
        val finishFailure = IllegalStateException("finish page failed")
        val closeFailure = IllegalStateException("Current page not finished!")
        val document = FakeDocument(closeFailure)
        val actual = assertThrows(IllegalStateException::class.java) {
            withClosePreservingPrimaryFailure(document, FakeDocument::close) {
                throw finishFailure
            }
        }
        assertSame(finishFailure, actual)
        assertSame(closeFailure, actual.suppressed.single())
        assertEquals(1, document.closes)
    }

    @Test fun closeFailureAfterSuccessfulWriteIsPropagated() {
        val closeFailure = IllegalStateException("close failed")
        val document = FakeDocument(closeFailure)
        val actual = assertThrows(IllegalStateException::class.java) {
            withClosePreservingPrimaryFailure(document, FakeDocument::close) { "written" }
        }
        assertSame(closeFailure, actual)
        assertEquals(1, document.closes)
    }

    @Test fun identicalPrimaryAndCloseThrowableIsNotSelfSuppressed() {
        val failure = IllegalStateException("same failure")
        val document = FakeDocument(failure)
        val actual = assertThrows(IllegalStateException::class.java) {
            withClosePreservingPrimaryFailure(document, FakeDocument::close) {
                throw failure
            }
        }
        assertSame(failure, actual)
        assertTrue(actual.suppressed.isEmpty())
        assertEquals(1, document.closes)
    }
}
