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
class BatchQueueTerminalCheckpointRecoveryTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("reset")
        BatchQueueRuntimeStore.clearQueue().getOrThrow()
    }

    private fun enqueue(name: String): Long {
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/$name.pdf")),
            emptyList()
        ).getOrThrow()
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    private suspend fun begin(): BatchRunAdmission =
        BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()

    @Test fun successfulTerminalSaveKeepsRunActiveForNextTask() = runBlocking {
        val id = enqueue("first")
        val run = begin()
        BatchQueueRuntimeStore.startNextQueuedTask(run.taskIds.toSet(), 1, run.runId)
        BatchQueueRuntimeStore.markTaskSuccess(id, "/out.pdf", 10L, run.runId)
        var writes = 0
        assertTrue(persistBatchTaskTerminalCheckpoint(
            persist = { writes++ }, taskIds = run.taskIds.toSet(),
            runId = run.runId, failureMessage = "save failed"
        ))
        assertEquals(1, writes)
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(run.taskIds.toSet(), run.runId))
    }

    @Test fun failedSaveAfterSuccessReleasesRunAndPreservesOutput() = runBlocking {
        val completedId = enqueue("completed")
        val queuedId = enqueue("queued")
        val run = begin()
        BatchQueueRuntimeStore.startNextQueuedTask(run.taskIds.toSet(), 2, run.runId)
        BatchQueueRuntimeStore.markTaskSuccess(completedId, "/result.pdf", 42L, run.runId)
        var writes = 0
        assertFalse(persistBatchTaskTerminalCheckpoint(
            persist = {
                writes++
                if (writes == 1) throw IllegalStateException("disk full")
            },
            taskIds = run.taskIds.toSet(), runId = run.runId,
            failureMessage = "Terminal checkpoint failed"
        ))
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(2, writes)
        assertFalse(state.isProcessing)
        assertEquals(BatchTaskStatus.SUCCESS, state.tasks.single { it.id == completedId }.status)
        assertEquals("/result.pdf", state.tasks.single { it.id == completedId }.outputPath)
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single { it.id == queuedId }.status)
        assertEquals(1, state.processedCount)
        assertEquals(1, state.successCount)
        assertEquals(0, state.failureCount)
        assertEquals("Terminal checkpoint failed", state.errorMessage)
    }

    @Test fun failedSaveAfterTaskFailureDoesNotDoubleCountEvenIfRecoveryFails() = runBlocking {
        val id = enqueue("failed")
        val run = begin()
        BatchQueueRuntimeStore.startNextQueuedTask(run.taskIds.toSet(), 1, run.runId)
        BatchQueueRuntimeStore.markTaskFailure(id, "conversion failed", run.runId)
        var writes = 0
        assertFalse(persistBatchTaskTerminalCheckpoint(
            persist = { writes++; throw IllegalStateException("storage offline") },
            taskIds = run.taskIds.toSet(), runId = run.runId,
            failureMessage = "Terminal checkpoint failed"
        ))
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(2, writes)
        assertFalse(state.isProcessing)
        assertEquals(BatchTaskStatus.FAILED, state.tasks.single().status)
        assertEquals("conversion failed", state.tasks.single().errorMessage)
        assertEquals(1, state.processedCount)
        assertEquals(0, state.successCount)
        assertEquals(1, state.failureCount)
        assertEquals("Terminal checkpoint failed", state.errorMessage)
    }

    @Test fun delayedFailureCannotEndNewRunOrRetryItsPersistence() = runBlocking {
        val completedId = enqueue("old")
        val queuedId = enqueue("retry")
        val old = begin()
        BatchQueueRuntimeStore.startNextQueuedTask(old.taskIds.toSet(), 2, old.runId)
        BatchQueueRuntimeStore.markTaskSuccess(completedId, "/old.pdf", 5L, old.runId)
        var newer: BatchRunAdmission? = null
        var writes = 0
        assertFalse(persistBatchTaskTerminalCheckpoint(
            persist = {
                writes++
                BatchQueueRuntimeStore.finishRunIfCurrent(old.taskIds.toSet(), old.runId, "old done")
                newer = begin()
                throw IllegalStateException("delayed old write")
            },
            taskIds = old.taskIds.toSet(), runId = old.runId,
            failureMessage = "old failure"
        ))
        val active = requireNotNull(newer)
        assertEquals(1, writes)
        assertEquals(listOf(queuedId), active.taskIds)
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(active.taskIds.toSet(), active.runId))
        assertEquals(null, BatchQueueRuntimeStore.state.value.errorMessage)
    }

    @Test fun repeatedOldFailureCannotChangeAlreadyFailedRun() = runBlocking {
        enqueue("task")
        val run = begin()
        BatchQueueRuntimeStore.startNextQueuedTask(run.taskIds.toSet(), 1, run.runId)
        BatchQueueRuntimeStore.markTaskFailure(run.taskIds.single(), "converter error", run.runId)
        var writes = 0
        suspend fun failSave() = persistBatchTaskTerminalCheckpoint(
            persist = { writes++; throw IllegalStateException("disk full") },
            taskIds = run.taskIds.toSet(), runId = run.runId,
            failureMessage = "save error"
        )
        assertFalse(failSave())
        val first = BatchQueueRuntimeStore.state.value
        assertFalse(failSave())
        assertEquals(3, writes)
        assertEquals(first, BatchQueueRuntimeStore.state.value)
        assertEquals(1, first.failureCount)
    }

    @Test fun cancelledSavePropagatesWithoutFailingRun() = runBlocking {
        enqueue("task")
        val run = begin()
        try {
            persistBatchTaskTerminalCheckpoint(
                persist = { throw CancellationException("cancelled") },
                taskIds = run.taskIds.toSet(), runId = run.runId,
                failureMessage = "must not report cancellation"
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(run.taskIds.toSet(), run.runId))
        assertEquals(null, BatchQueueRuntimeStore.state.value.errorMessage)
    }
}
