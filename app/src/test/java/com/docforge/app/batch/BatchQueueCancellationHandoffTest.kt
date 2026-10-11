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

    private fun admit(): Set<Long> = runBlocking {
        BatchQueueRuntimeStore.beginProcessing().getOrThrow().toSet()
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
        val ids = admit()
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(ids))
        assertFalse(BatchQueueRuntimeStore.state.value.isProcessing)
        assertEquals(BatchTaskStatus.CANCELED, BatchQueueRuntimeStore.state.value.tasks.single().status)
        assertFalse(BatchQueueRuntimeStore.isCurrentRun(ids))
        assertEquals(first, ids.single())
    }

    @Test fun laterEnqueuedTasksAreNeverCanceledByOldRun() {
        val first = enqueue("one")
        val ids = admit()
        val later = enqueue("later")
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(ids))
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.CANCELED, state.tasks.single { it.id == first }.status)
        assertEquals(BatchTaskStatus.QUEUED, state.tasks.single { it.id == later }.status)
    }

    @Test fun interruptedRunningTaskIsCanceledOnce() {
        val taskId = enqueue("running")
        val ids = admit()
        assertEquals(taskId, BatchQueueRuntimeStore.startNextQueuedTask(ids, 1)?.id)
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(ids))
        assertEquals(BatchTaskStatus.CANCELED, BatchQueueRuntimeStore.state.value.tasks.single().status)
        assertEquals(1, BatchQueueRuntimeStore.state.value.processedCount)
        assertEquals(1, BatchQueueRuntimeStore.state.value.failureCount)
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(ids))
        assertEquals(1, BatchQueueRuntimeStore.state.value.failureCount)
    }

    @Test fun secondCancellationAndLateFinishAreNoOps() {
        enqueue("one")
        val ids = admit()
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(ids))
        val state = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(ids))
        assertFalse(BatchQueueRuntimeStore.finishRunIfCurrent(ids, "stale"))
        assertEquals(state, BatchQueueRuntimeStore.state.value)
    }

    @Test fun lateCancelDoesNotFinalizeNewRun() {
        enqueue("first")
        val first = admit()
        assertTrue(BatchQueueRuntimeStore.cancelPendingRun(first))
        val secondId = enqueue("second")
        val second = admit()
        assertEquals(setOf(secondId), second)
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(first))
        assertFalse(BatchQueueRuntimeStore.finishRunIfCurrent(first, "old"))
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(second))
        assertTrue(BatchQueueRuntimeStore.state.value.isProcessing)
    }

    @Test fun normalCompletionIsGuardedAndIdempotent() {
        enqueue("first")
        val ids = admit()
        assertTrue(BatchQueueRuntimeStore.finishRunIfCurrent(ids, "complete"))
        assertFalse(BatchQueueRuntimeStore.finishRunIfCurrent(ids, "again"))
        assertEquals("complete", BatchQueueRuntimeStore.state.value.statusMessage)
        assertFalse(BatchQueueRuntimeStore.state.value.isProcessing)
    }

    @Test fun emptyOrWrongRunCannotCancel() {
        enqueue("first")
        val ids = admit()
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(emptySet()))
        assertFalse(BatchQueueRuntimeStore.cancelPendingRun(setOf(-1)))
        assertTrue(BatchQueueRuntimeStore.isCurrentRun(ids))
    }
}
