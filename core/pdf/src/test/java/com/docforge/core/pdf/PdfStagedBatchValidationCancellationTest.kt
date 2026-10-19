package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfStagedBatchValidationCancellationTest {
    @Test fun cancelledBatchSkipsValidation() = runBlocking {
        var called = false
        var cancelled = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            coroutineContext.cancel()
            try {
                withStagedValidationPreflight { called = true }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertTrue(cancelled)
        assertFalse(called)
    }

    @Test fun cancellationDuringValidationStopsNormalReturn() = runBlocking {
        var calls = 0
        var cancelled = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withStagedValidationPreflight {
                    calls++
                    coroutineContext.cancel()
                    "done"
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertTrue(cancelled)
        assertEquals(1, calls)
    }

    @Test fun cancelledFirstValidationSkipsSecond() = runBlocking {
        val calls = mutableListOf<String>()
        var cancelled = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withStagedValidationPreflight {
                    calls += "first"
                    coroutineContext.cancel()
                }
                withStagedValidationPreflight { calls += "second" }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertTrue(cancelled)
        assertEquals(listOf("first"), calls)
    }

    @Test fun activeBatchPreservesValidationOrder() = runBlocking {
        val calls = mutableListOf<String>()
        withStagedValidationPreflight { calls += "first" }
        withStagedValidationPreflight { calls += "second" }
        assertEquals(listOf("first", "second"), calls)
    }

    @Test fun activeValidationPreservesReturnValue() = runBlocking {
        assertEquals("ok", withStagedValidationPreflight { "ok" })
    }

    @Test fun activeValidationPreservesExceptionIdentity() = runBlocking {
        val original = IllegalStateException("invalid stage")
        try {
            withStagedValidationPreflight<Unit> { throw original }
            fail("Expected original failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
    }

    @Test fun activeValidationAllowsNullResult() = runBlocking {
        assertEquals(null, withStagedValidationPreflight<String?> { null })
    }
}
