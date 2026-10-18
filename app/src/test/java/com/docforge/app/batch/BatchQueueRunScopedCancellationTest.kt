package com.docforge.app.batch

import android.net.Uri
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueRunScopedCancellationTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun enqueue(index: Int): Long {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/cancel-$index.pdf")),
            emptyList()
        ).isSuccess)
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    private fun begin(): Set<Long> = runBlocking {
        BatchQueueRuntimeStore.beginProcessing().getOrThrow().toSet()
    }

    private fun status(id: Long): BatchTaskStatus =
        BatchQueueRuntimeStore.state.value.tasks.single { it.id == id }.status

    @Test fun cancelOnlyTasksScheduledForCurrentRun() {
        val first = enqueue(1)
        val second = enqueue(2)
        val runIds = begin()
        val addedAfterStart = enqueue(3)

        BatchQueueRuntimeStore.markRemainingQueuedAsCanceled(runIds)

        assertEquals(BatchTaskStatus.CANCELED, status(first))
        assertEquals(BatchTaskStatus.CANCELED, status(second))
        assertEquals(BatchTaskStatus.QUEUED, status(addedAfterStart))
        assertEquals(0, BatchQueueRuntimeStore.state.value.processedCount)
    }

    @Test fun cancellationPreservesRunningAndTerminalTasks() {
        val running = enqueue(4)
        val completed = enqueue(5)
        val queued = enqueue(6)
        val runIds = begin()
        assertEquals(running, BatchQueueRuntimeStore.startNextQueuedTask(setOf(running), 3)?.id)
        assertEquals(completed, BatchQueueRuntimeStore.startNextQueuedTask(setOf(completed), 3)?.id)
        BatchQueueRuntimeStore.markTaskSuccess(completed, "/done.pdf", 10L)
        val outsideRun = enqueue(7)

        BatchQueueRuntimeStore.markRemainingQueuedAsCanceled(runIds)

        assertEquals(BatchTaskStatus.RUNNING, status(running))
        assertEquals(BatchTaskStatus.SUCCESS, status(completed))
        assertEquals(BatchTaskStatus.CANCELED, status(queued))
        assertEquals(BatchTaskStatus.QUEUED, status(outsideRun))
        assertEquals(1, BatchQueueRuntimeStore.state.value.processedCount)
    }

    @Test fun emptyEligibleSetCannotCancelAnything() {
        val original = enqueue(8)
        begin()
        val before = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markRemainingQueuedAsCanceled(emptySet())
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertEquals(BatchTaskStatus.QUEUED, status(original))
    }

    @Test fun concurrentEnqueueAndCancelNeverCancelNewTask() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(100) { index ->
                reset()
                val original = enqueue(1000 + index)
                val runIds = begin()
                val gate = CountDownLatch(1)
                val added = executor.submit<Long> {
                    gate.await()
                    enqueue(2000 + index)
                }
                val canceled = executor.submit {
                    gate.await()
                    BatchQueueRuntimeStore.markRemainingQueuedAsCanceled(runIds)
                }
                gate.countDown()
                val newId = added.get(10, TimeUnit.SECONDS)
                canceled.get(10, TimeUnit.SECONDS)
                assertEquals(BatchTaskStatus.CANCELED, status(original))
                assertEquals(BatchTaskStatus.QUEUED, status(newId))
                assertEquals(2, BatchQueueRuntimeStore.state.value.tasks.size)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test fun cancellationPreservesFailedAndAlreadyCanceledTasks() {
        val failed = enqueue(9)
        val alreadyCanceled = enqueue(10)
        val queued = enqueue(11)
        val runIds = begin()
        assertEquals(failed, BatchQueueRuntimeStore.startNextQueuedTask(setOf(failed), 3)?.id)
        BatchQueueRuntimeStore.markTaskFailure(failed, "original failure")
        assertEquals(alreadyCanceled, BatchQueueRuntimeStore.startNextQueuedTask(setOf(alreadyCanceled), 3)?.id)
        BatchQueueRuntimeStore.markTaskCanceled(alreadyCanceled)

        BatchQueueRuntimeStore.markRemainingQueuedAsCanceled(runIds)

        assertEquals(BatchTaskStatus.FAILED, status(failed))
        assertEquals("original failure", BatchQueueRuntimeStore.state.value.tasks.single { it.id == failed }.errorMessage)
        assertEquals(BatchTaskStatus.CANCELED, status(alreadyCanceled))
        assertEquals("Task canceled", BatchQueueRuntimeStore.state.value.tasks.single { it.id == alreadyCanceled }.errorMessage)
        assertEquals(BatchTaskStatus.CANCELED, status(queued))
        assertEquals(2, BatchQueueRuntimeStore.state.value.processedCount)
        assertEquals(0, BatchQueueRuntimeStore.state.value.successCount)
        assertEquals(2, BatchQueueRuntimeStore.state.value.failureCount)
    }
}
