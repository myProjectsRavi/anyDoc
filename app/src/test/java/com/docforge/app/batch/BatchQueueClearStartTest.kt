package com.docforge.app.batch

import android.net.Uri
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
class BatchQueueClearStartTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun enqueue(index: Int): Long {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/clear-start-$index.pdf")),
            emptyList()
        ).isSuccess)
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    @Test fun clearNonProcessingQueueResetsTasksAndCounters() {
        val id = enqueue(1)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1)
        BatchQueueRuntimeStore.markTaskSuccess(id, "/done.pdf", 10L)
        BatchQueueRuntimeStore.finishProcessing("Finished")
        assertEquals(1, BatchQueueRuntimeStore.state.value.processedCount)

        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
        val state = BatchQueueRuntimeStore.state.value
        assertFalse(state.isProcessing)
        assertTrue(state.tasks.isEmpty())
        assertEquals(0, state.processedCount)
        assertEquals("Queue cleared", state.statusMessage)
    }

    @Test fun clearWhileProcessingFailsWithoutChangingState() {
        enqueue(2)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(BatchQueueRuntimeStore.clearQueue().isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun beginEmptyQueueFailsWithoutChangingState() {
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun beginReturnsCurrentQueuedIdsAndRejectsDuplicateBegin() {
        val first = enqueue(3)
        val second = enqueue(4)
        val started = runBlocking { BatchQueueRuntimeStore.beginProcessing() }
        assertEquals(listOf(first, second), started.getOrThrow())
        assertTrue(BatchQueueRuntimeStore.state.value.isProcessing)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isFailure)
        assertEquals(listOf(first, second), BatchQueueRuntimeStore.state.value.tasks.map { it.id })
    }

    @Test fun concurrentClearAndBeginCannotDiscardStartedTasks() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(150) { index ->
                reset()
                val id = enqueue(1000 + index)
                val gate = CountDownLatch(1)
                val start = pool.submit<Result<List<Long>>> {
                    gate.await()
                    runBlocking { BatchQueueRuntimeStore.beginProcessing() }
                }
                val clear = pool.submit<Result<Unit>> {
                    gate.await()
                    BatchQueueRuntimeStore.clearQueue()
                }
                gate.countDown()
                val started = start.get(10, TimeUnit.SECONDS)
                val cleared = clear.get(10, TimeUnit.SECONDS)
                assertTrue(started.isSuccess != cleared.isSuccess)
                val state = BatchQueueRuntimeStore.state.value
                if (started.isSuccess) {
                    assertTrue(state.isProcessing)
                    assertEquals(listOf(id), started.getOrThrow())
                    assertEquals(listOf(id), state.tasks.map { it.id })
                } else {
                    assertFalse(state.isProcessing)
                    assertTrue(state.tasks.isEmpty())
                }
            }
        } finally {
            pool.shutdownNow()
        }
    }
}
