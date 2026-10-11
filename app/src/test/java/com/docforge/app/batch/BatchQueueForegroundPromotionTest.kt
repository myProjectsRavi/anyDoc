package com.docforge.app.batch

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueForegroundPromotionTest {
    @Before fun resetBefore() = reset()
    @After fun resetAfter() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("reset")
        BatchQueueRuntimeStore.clearQueue().getOrThrow()
    }

    private fun admit(): BatchRunAdmission = runBlocking {
        BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()
    }

    private fun enqueue() {
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/document.pdf")),
            emptyList()
        ).getOrThrow()
    }

    @Test fun successfulPromotionLeavesRunActiveAndDoesNotReportFailure() {
        enqueue()
        val admitted = admit()
        var calls = 0
        val promoted = attemptBatchForegroundPromotion(
            promote = { calls++ },
            onFailure = { fail("Unexpected error: $it") }
        )
        assertTrue(promoted)
        assertEquals(1, calls)
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(admitted.taskIds.toSet(), admitted.runId))
    }

    @Test fun securityExceptionFailsOnlyAdmittedRunAndLeavesTasksRetryable() {
        enqueue()
        val admitted = admit()
        val promoted = attemptBatchForegroundPromotion(
            promote = { throw SecurityException("foreground denied") },
            onFailure = { error ->
                assertTrue(BatchQueueRuntimeStore.failRunIfCurrent(
                    admitted.taskIds.toSet(), admitted.runId,
                    "Unable to promote: ${error.message}"
                ))
            }
        )
        assertFalse(promoted)
        val state = BatchQueueRuntimeStore.state.value
        assertFalse(state.isProcessing)
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single().status)
        assertTrue(state.errorMessage?.contains("foreground denied") == true)
    }

    @Test fun platformStartRestrictionIsHandledWithoutThrowing() {
        var errors = 0
        val promoted = attemptBatchForegroundPromotion(
            promote = { throw IllegalStateException("foreground restricted") },
            onFailure = { errors++; assertEquals("foreground restricted", it.message) }
        )
        assertFalse(promoted)
        assertEquals(1, errors)
    }

    @Test fun obsoletePromotionFailureCannotStopRetryWithSameTaskIds() {
        enqueue()
        val old = admit()
        BatchQueueRuntimeStore.failProcessing("previous worker rejected")
        val newer = admit()
        assertEquals(old.taskIds, newer.taskIds)
        val before = BatchQueueRuntimeStore.state.value
        val promoted = attemptBatchForegroundPromotion(
            promote = { throw SecurityException("old promotion failed") },
            onFailure = { error ->
                assertFalse(BatchQueueRuntimeStore.failRunIfCurrent(
                    old.taskIds.toSet(), old.runId, error.message ?: "error"
                ))
            }
        )
        assertFalse(promoted)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(newer.taskIds.toSet(), newer.runId))
    }

    @Test fun coroutineCancellationPropagatesUnchanged() {
        var failures = 0
        try {
            attemptBatchForegroundPromotion(
                promote = { throw CancellationException("cancelled") },
                onFailure = { failures++ }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
        assertEquals(0, failures)
    }
}
