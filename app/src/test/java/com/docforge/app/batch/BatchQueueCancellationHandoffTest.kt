package com.docforge.app.batch

import android.net.Uri
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
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
class BatchQueueCancellationHandoffTest {
    @Before fun setup() = reset()
    @After fun cleanup() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("reset")
        BatchQueueRuntimeStore.clearQueue().getOrThrow()
    }

    private fun enqueue(name: String): Long {
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/$name.pdf")), emptyList()
        ).getOrThrow()
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    private fun admit(): BatchRunAdmission = runBlocking {
        BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()
    }

    @Test fun atomicJobRunsCancellationHandlerEvenBeforeDispatch() = runBlocking {
        val pending = ArrayDeque<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                pending.addLast(block)
            }
        }
        var handled = false
        val job = CoroutineScope(dispatcher).launch(start = CoroutineStart.ATOMIC) {
            try {
                currentCoroutineContext().ensureActive()
            } catch (_: CancellationException) {
                handled = true
            }
        }
        job.cancel()
        assertFalse(handled)
        assertEquals(1, pending.size)
        pending.removeFirst().run()
        job.join()
        assertTrue(handled)
    }

    @Test fun cancellationBeforeWorkerAttachmentFinishesRun() {
        val first = enqueue("one")
        val session = admit()
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(session.taskIds.toSet(), session.runId))
        assertFalse(BatchQueueRuntimeStore.state.value.isProcessing)
        assertEquals(BatchTaskStatus.CANCELED, BatchQueueRuntimeStore.state.value.tasks.single().status)
        assertFalse(BatchQueueRuntimeStore.isCurrentRun(session.taskIds.toSet(), session.runId))
        assertEquals(first, session.taskIds.single())
    }

    @Test fun laterEnqueuedTasksAreNeverCanceledByOldRun() {
        val first = enqueue("one")
        val session = admit()
        val later = enqueue("later")
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(session.taskIds.toSet(), session.runId))
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.CANCELED, state.tasks.single { it.id == first }.status)
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single { it.id == later }.status)
    }

    @Test fun interruptedRunningTaskIsCanceledOnce() {
        val id = enqueue("running")
        val session = admit()
        assertEquals(id, BatchQueueRuntimeStore.startNextQueuedTask(session.taskIds.toSet(), 1)?.id)
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(session.taskIds.toSet(), session.runId))
        assertEquals(BatchTaskStatus.CANCELED, BatchQueueRuntimeStore.state.value.tasks.single().status)
        assertEquals(1, BatchQueueRuntimeStore.state.value.processedCount)
        assertEquals(1, BatchQueueRuntimeStore.state.value.failureCount)
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(session.taskIds.toSet(), session.runId))
        assertEquals(1, BatchQueueRuntimeStore.state.value.failureCount)
    }

    @Test fun secondCancellationAndLateFinishAreNoOps() {
        enqueue("one")
        val session = admit()
        val ids = session.taskIds.toSet()
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(ids, session.runId))
        val state = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(ids, session.runId))
        assertFalse(BatchQueueRuntimeStore.finishRunIfCurrent(ids, session.runId, "stale"))
        assertEquals(state, BatchQueueRuntimeStore.state.value)
    }

    @Test fun lateCancelDoesNotFinalizeNewRun() {
        enqueue("first")
        val first = admit()
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(first.taskIds.toSet(), first.runId))
        val secondId = enqueue("second")
        val second = admit()
        assertEquals(setOf(secondId), second.taskIds.toSet())
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(first.taskIds.toSet(), first.runId))
        assertFalse(BatchQueueRuntimeStore.finishRunIfCurrent(first.taskIds.toSet(), first.runId, "old"))
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(second.taskIds.toSet(), second.runId))
        assertTrue(BatchQueueRuntimeStore.state.value.isProcessing)
    }

    @Test fun retryWithIdenticalTaskIdsStillHasDistinctRunIdentity() {
        enqueue("retry")
        val first = admit()
        BatchQueueRuntimeStore.failProcessing("start failed")
        val second = admit()
        assertEquals(first.taskIds, second.taskIds)
        assertTrue(first.runId != second.runId)
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(first.taskIds.toSet(), first.runId))
        assertFalse(BatchQueueRuntimeStore.finishRunIfCurrent(first.taskIds.toSet(), first.runId, "stale"))
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(second.taskIds.toSet(), second.runId))
    }

    @Test fun oldWorkerFailureCannotClearRetryWithSameTasks() {
        enqueue("retry")
        val first = admit()
        BatchQueueRuntimeStore.failProcessing("start failed")
        val second = admit()
        val before = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.failRunIfCurrent(
            first.taskIds.toSet(), first.runId, "old worker failed"
        ))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(second.taskIds.toSet(), second.runId))
    }

    @Test fun delayedTerminalSaveFailureCannotStopActiveRun() {
        enqueue("one")
        val first = admit()
        assertTrue(BatchQueueRuntimeStore.finishRunIfCurrent(
            first.taskIds.toSet(), first.runId, "done"
        ))
        val second = admit()
        val before = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.reportPersistenceFailureIfIdle("old save failed"))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(second.taskIds.toSet(), second.runId))
    }

    @Test fun staleWorkerCannotClaimTaskInRetryWithSameTaskIds() {
        val id = enqueue("retry")
        val first = admit()
        BatchQueueRuntimeStore.failProcessing("previous start failed")
        val second = admit()
        assertEquals(first.taskIds, second.taskIds)
        val before = BatchQueueRuntimeStore.state.value
        assertEquals(null, BatchQueueRuntimeStore.startNextQueuedTask(
            first.taskIds.toSet(), 1, first.runId
        ))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertEquals(id, BatchQueueRuntimeStore.startNextQueuedTask(
            second.taskIds.toSet(), 1, second.runId
        )?.id)
    }

    @Test fun staleWorkerCannotCompleteNewRunTaskAfterRetry() {
        val id = enqueue("retry")
        val first = admit()
        BatchQueueRuntimeStore.failProcessing("previous start failed")
        val second = admit()
        assertEquals(id, BatchQueueRuntimeStore.startNextQueuedTask(
            second.taskIds.toSet(), 1, second.runId
        )?.id)
        val before = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markTaskSuccess(id, "/old.pdf", 10L, first.runId)
        BatchQueueRuntimeStore.markTaskFailure(id, "old error", first.runId)
        BatchQueueRuntimeStore.markTaskCanceled(id, first.runId)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        BatchQueueRuntimeStore.markTaskSuccess(id, "/new.pdf", 11L, second.runId)
        val after = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.SUCCESS, after.tasks.single().status)
        assertEquals(1, after.processedCount)
        assertEquals(1, after.successCount)
    }

    @Test fun normalCompletionIsGuardedAndIdempotent() {
        enqueue("first")
        val session = admit()
        assertTrue(BatchQueueRuntimeStore.finishRunIfCurrent(session.taskIds.toSet(), session.runId, "complete"))
        assertFalse(BatchQueueRuntimeStore.finishRunIfCurrent(session.taskIds.toSet(), session.runId, "again"))
        assertEquals("complete", BatchQueueRuntimeStore.state.value.statusMessage)
        assertFalse(BatchQueueRuntimeStore.state.value.isProcessing)
    }

    @Test fun emptyWrongIdOrWrongRunCannotCancel() {
        enqueue("first")
        val session = admit()
        val ids = session.taskIds.toSet()
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(emptySet(), session.runId))
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(setOf(-1), session.runId))
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(ids, session.runId + 1))
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(ids, session.runId))
    }
}
