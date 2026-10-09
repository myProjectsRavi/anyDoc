package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BatchQueueCheckpointTest {
    @Test fun successfulWriteDoesNotCallFailureHandler() = runBlocking {
        var writes = 0
        var failures = 0
        val saved = persistBatchQueueCheckpoint(
            persist = { writes++ },
            onFailure = { failures++ }
        )
        assertTrue(saved)
        assertEquals(1, writes)
        assertEquals(0, failures)
    }

    @Test fun ordinaryWriteFailureIsReportedAndReturnsFalse() = runBlocking {
        val error = IllegalStateException("storage unavailable")
        var received: Exception? = null
        val saved = persistBatchQueueCheckpoint(
            persist = { throw error },
            onFailure = { received = it }
        )
        assertFalse(saved)
        assertSame(error, received)
    }

    @Test fun cancellationFromWriterPropagatesWithoutFailureHandler() = runBlocking {
        var failures = 0
        try {
            persistBatchQueueCheckpoint(
                persist = { throw CancellationException("cancelled during write") },
                onFailure = { failures++ }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled during write", expected.message)
        }
        assertEquals(0, failures)
    }

    @Test fun cancellationFromFailureHandlerAlsoPropagates() = runBlocking {
        try {
            persistBatchQueueCheckpoint(
                persist = { throw IllegalStateException("disk failure") },
                onFailure = { throw CancellationException("cancelled during recovery") }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled during recovery", expected.message)
        }
    }

    @Test fun recoveryWriteFailureDoesNotOverrideOriginalFailure() = runBlocking {
        val original = IllegalStateException("first snapshot write failed")
        var writes = 0
        var handled: Exception? = null

        val saved = persistBatchQueueCheckpoint(
            persist = {
                writes++
                throw original
            },
            onFailure = { error ->
                handled = error
                persistBatchQueueCheckpoint(
                    persist = { writes++; error("retry failed") },
                    onFailure = { /* Retain the original failure. */ }
                )
            }
        )

        assertFalse(saved)
        assertSame(original, handled)
        assertEquals(2, writes)
    }

    @Test fun cancellingSuspendedWriteDoesNotInvokeFailureHandler() = runBlocking {
        var failures = 0
        val job = launch {
            persistBatchQueueCheckpoint(
                persist = { awaitCancellation() },
                onFailure = { failures++ }
            )
        }
        yield()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertEquals(0, failures)
    }
}
