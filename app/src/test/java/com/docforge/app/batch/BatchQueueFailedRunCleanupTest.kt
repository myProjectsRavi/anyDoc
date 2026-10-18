package com.docforge.app.batch

import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueFailedRunCleanupTest {
    @Before fun setup() = reset()
    @After fun cleanup() = reset()

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

    private fun begin(): BatchRunAdmission = runBlocking {
        BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()
    }

    @Test fun activeFailureTerminalizesOnlyRunningTaskAndKeepsQueuedWork() {
        val runningId = enqueue("running")
        val queuedId = enqueue("queued")
        val run = begin()
        assertEquals(runningId, BatchQueueRuntimeStore.startNextQueuedTask(
            run.taskIds.toSet(), 2, run.runId
        )?.id)

        assertTrue(BatchQueueRuntimeStore.failRunIfCurrent(
            run.taskIds.toSet(), run.runId, "storage failure"
        ))

        val state = BatchQueueRuntimeStore.state.value
        assertFalse(state.isProcessing)
        assertEquals(BatchTaskStatus.FAILED, state.tasks.single { it.id == runningId }.status)
        assertTrue(state.tasks.single { it.id == runningId }.errorMessage!!.contains("storage failure"))
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single { it.id == queuedId }.status)
        assertEquals(1, state.processedCount)
        assertEquals(1, state.failureCount)
        assertEquals(0, state.successCount)
        assertTrue(state.errorMessage!!.contains("storage failure"))
        // The interrupted row is no longer non-removable; queued work remains runnable.
        assertTrue(BatchQueueRuntimeStore.removeTask(runningId).isSuccess)
        val retry = begin()
        assertEquals(listOf(queuedId), retry.taskIds)
    }

    @Test fun completedResultsAndLaterEnqueuesArePreserved() {
        val completedId = enqueue("complete")
        val interruptedId = enqueue("interrupted")
        val waitingId = enqueue("waiting")
        val run = begin()
        assertEquals(completedId, BatchQueueRuntimeStore.startNextQueuedTask(
            run.taskIds.toSet(), 3, run.runId
        )?.id)
        BatchQueueRuntimeStore.markTaskSuccess(completedId, "/finished.pdf", 120L, run.runId)
        assertEquals(interruptedId, BatchQueueRuntimeStore.startNextQueuedTask(
            run.taskIds.toSet(), 3, run.runId
        )?.id)
        val laterId = enqueue("later")

        assertTrue(BatchQueueRuntimeStore.failRunIfCurrent(
            run.taskIds.toSet(), run.runId, "unexpected abort"
        ))
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.SUCCESS, state.tasks.single { it.id == completedId }.status)
        assertEquals("/finished.pdf", state.tasks.single { it.id == completedId }.outputPath)
        assertEquals(BatchTaskStatus.FAILED, state.tasks.single { it.id == interruptedId }.status)
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single { it.id == waitingId }.status)
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single { it.id == laterId }.status)
        assertEquals(2, state.processedCount)
        assertEquals(1, state.successCount)
        assertEquals(1, state.failureCount)
    }

    @Test fun repeatedFailureIsNoOpAndDoesNotDoubleCount() {
        enqueue("running")
        val run = begin()
        BatchQueueRuntimeStore.startNextQueuedTask(run.taskIds.toSet(), 1, run.runId)
        assertTrue(BatchQueueRuntimeStore.failRunIfCurrent(
            run.taskIds.toSet(), run.runId, "first error"
        ))
        val after = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.failRunIfCurrent(
            run.taskIds.toSet(), run.runId, "later error"
        ))
        assertEquals(after, BatchQueueRuntimeStore.state.value)
        assertEquals(1, after.failureCount)
    }

    @Test fun staleFailureCannotMutateNewRunEvenWithSameTaskIds() {
        enqueue("retry")
        val old = begin()
        assertTrue(BatchQueueRuntimeStore.failRunIfCurrent(
            old.taskIds.toSet(), old.runId, "failed to start"
        ))
        val newer = begin()
        assertEquals(old.taskIds, newer.taskIds)
        val before = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.failRunIfCurrent(
            old.taskIds.toSet(), old.runId, "stale failure"
        ))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(newer.taskIds.toSet(), newer.runId))
    }

    @Test fun failureBeforeAnyTaskStartedPreservesQueuedTasksAndCounts() {
        enqueue("queued")
        val run = begin()
        assertTrue(BatchQueueRuntimeStore.failRunIfCurrent(
            run.taskIds.toSet(), run.runId, "foreground denied"
        ))
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single().status)
        assertEquals(0, state.processedCount)
        assertEquals(0, state.failureCount)
    }
}
