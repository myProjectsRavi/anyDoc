package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompareCleanupFailureTest {
    @Test fun successReturnsResultWithoutCleanup() {
        var cleanups = 0
        val result = withCleanupOnFailure(Any(), { cleanups++ }) { "rendered" }
        assertEquals("rendered", result)
        assertEquals(0, cleanups)
    }

    @Test fun primaryFailureTriggersCleanupOnce() {
        val failure = IllegalStateException("render failed")
        var cleanups = 0
        val actual = assertThrows(IllegalStateException::class.java) {
            withCleanupOnFailure<Any, Unit>(Any(), { cleanups++ }) { throw failure }
        }
        assertSame(failure, actual)
        assertEquals(1, cleanups)
        assertTrue(actual.suppressed.isEmpty())
    }

    @Test fun differentCleanupFailureIsSuppressed() {
        val failure = IllegalStateException("render failed")
        val cleanupFailure = IllegalArgumentException("recycle failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            withCleanupOnFailure<Any, Unit>(Any(), { throw cleanupFailure }) { throw failure }
        }
        assertSame(failure, actual)
        assertSame(cleanupFailure, actual.suppressed.single())
    }

    @Test fun identicalCleanupFailureDoesNotMaskOriginal() {
        val failure = IllegalStateException("same instance")
        var cleanups = 0
        val actual = assertThrows(IllegalStateException::class.java) {
            withCleanupOnFailure<Any, Unit>(Any(), {
                cleanups++
                throw failure
            }) { throw failure }
        }
        assertSame(failure, actual)
        assertEquals(1, cleanups)
        assertTrue(actual.suppressed.isEmpty())
    }

    @Test fun cancellationRemainsPrimaryWhenCleanupFails() {
        val cancellation = CancellationException("cancelled")
        val cleanupFailure = IllegalStateException("recycle failed")
        val actual = assertThrows(CancellationException::class.java) {
            withCleanupOnFailure<Any, Unit>(Any(), { throw cleanupFailure }) {
                throw cancellation
            }
        }
        assertSame(cancellation, actual)
        assertSame(cleanupFailure, actual.suppressed.single())
    }
}
