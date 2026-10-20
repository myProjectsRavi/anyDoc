package com.docforge.app.batch

import android.net.Uri
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueTerminalTransitionTest {
    @Before fun setUp() {
        resetQueue()
    }

    @After fun tearDown() {
        resetQueue()
    }

    private fun resetQueue() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun addQueuedTask(): Long {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/input.pdf")),
            emptyList()
        ).isSuccess)
        return BatchQueueRuntimeStore.state.value.tasks.single().id
    }

    private fun startTask(): Long {
        val id = addQueuedTask()
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1))
        return id
    }

    private fun assertCounts(processed: Int, success: Int, failure: Int) {
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(processed, state.processedCount)
        assertEquals(success, state.successCount)
        assertEquals(failure, state.failureCount)
    }

    @Test fun successUpdatesTaskAndCountersTogether() {
        val id = startTask()
        BatchQueueRuntimeStore.markTaskSuccess(id, "/output/result.pdf", 42L)
        val task = BatchQueueRuntimeStore.state.value.tasks.single()
        assertEquals(BatchTaskStatus.SUCCESS, task.status)
        assertEquals("/output/result.pdf", task.outputPath)
        assertEquals(42L, task.outputSizeBytes)
        assertEquals(null, task.errorMessage)
        assertCounts(1, 1, 0)
    }

    @Test fun failureUpdatesTaskAndCountersTogether() {
        val id = startTask()
        BatchQueueRuntimeStore.markTaskFailure(id, "failed")
        val task = BatchQueueRuntimeStore.state.value.tasks.single()
        assertEquals(BatchTaskStatus.FAILED, task.status)
        assertEquals("failed", task.errorMessage)
        assertEquals(null, task.outputPath)
        assertCounts(1, 0, 1)
    }

    @Test fun cancellationUpdatesTaskAndCountersTogether() {
        val id = startTask()
        BatchQueueRuntimeStore.markTaskCanceled(id)
        val task = BatchQueueRuntimeStore.state.value.tasks.single()
        assertEquals(BatchTaskStatus.CANCELED, task.status)
        assertEquals("Task canceled", task.errorMessage)
        assertCounts(1, 0, 1)
    }

    @Test fun duplicateSuccessDoesNotReplaceOutputOrIncrementAgain() {
        val id = startTask()
        BatchQueueRuntimeStore.markTaskSuccess(id, "/first.pdf", 10L)
        val first = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markTaskSuccess(id, "/second.pdf", 20L)
        assertEquals(first, BatchQueueRuntimeStore.state.value)
        assertCounts(1, 1, 0)
    }

    @Test fun conflictingFailureAndCancellationCannotOverwriteSuccess() {
        val id = startTask()
        BatchQueueRuntimeStore.markTaskSuccess(id, "/first.pdf", 10L)
        val first = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markTaskFailure(id, "late failure")
        BatchQueueRuntimeStore.markTaskCanceled(id)
        assertEquals(first, BatchQueueRuntimeStore.state.value)
        assertCounts(1, 1, 0)
    }

    @Test fun conflictingSuccessCannotOverwriteFailure() {
        val id = startTask()
        BatchQueueRuntimeStore.markTaskFailure(id, "original failure")
        val first = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markTaskSuccess(id, "/late.pdf", 10L)
        BatchQueueRuntimeStore.markTaskCanceled(id)
        assertEquals(first, BatchQueueRuntimeStore.state.value)
        assertCounts(1, 0, 1)
    }

    @Test fun duplicateCancellationCannotBecomeFailure() {
        val id = startTask()
        BatchQueueRuntimeStore.markTaskCanceled(id)
        val first = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markTaskCanceled(id)
        BatchQueueRuntimeStore.markTaskFailure(id, "late")
        assertEquals(first, BatchQueueRuntimeStore.state.value)
        assertCounts(1, 0, 1)
    }

    @Test fun queuedTaskCannotBeCompletedWithoutRunning() {
        val id = addQueuedTask()
        val before = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markTaskSuccess(id, "/invalid.pdf", 1L)
        BatchQueueRuntimeStore.markTaskFailure(id, "invalid")
        BatchQueueRuntimeStore.markTaskCanceled(id)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertCounts(0, 0, 0)
    }

    @Test fun unknownTaskCannotChangeCounters() {
        val id = startTask()
        val before = BatchQueueRuntimeStore.state.value
        BatchQueueRuntimeStore.markTaskSuccess(id + 100, "/invalid.pdf", 1L)
        BatchQueueRuntimeStore.markTaskFailure(id + 100, "invalid")
        BatchQueueRuntimeStore.markTaskCanceled(id + 100)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertCounts(0, 0, 0)
    }

    @Test fun concurrentTerminalCallbacksProduceExactlyOneOutcome() {
        val executor = Executors.newFixedThreadPool(3)
        try {
            repeat(50) {
                resetQueue()
                val id = startTask()
                val gate = CountDownLatch(1)
                val futures = listOf(
                    executor.submit { gate.await(); BatchQueueRuntimeStore.markTaskSuccess(id, "/ok.pdf", 10L) },
                    executor.submit { gate.await(); BatchQueueRuntimeStore.markTaskFailure(id, "failed") },
                    executor.submit { gate.await(); BatchQueueRuntimeStore.markTaskCanceled(id) }
                )
                gate.countDown()
                futures.forEach { it.get(5, TimeUnit.SECONDS) }
                val state = BatchQueueRuntimeStore.state.value
                val task = state.tasks.single()
                assertTrue(task.status in setOf(
                    BatchTaskStatus.SUCCESS, BatchTaskStatus.FAILED, BatchTaskStatus.CANCELED
                ))
                assertEquals(1, state.processedCount)
                assertEquals(1, state.successCount + state.failureCount)
                assertEquals(if (task.status == BatchTaskStatus.SUCCESS) 1 else 0, state.successCount)
                assertEquals(if (task.status == BatchTaskStatus.SUCCESS) 0 else 1, state.failureCount)
            }
        } finally {
            executor.shutdownNow()
        }
    }
}
