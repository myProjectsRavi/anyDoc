package com.docforge.app.batch

import android.net.Uri
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueReorderTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun add(index: Int): Long {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/reorder-$index.pdf")),
            emptyList()
        ).isSuccess)
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    private fun ids(): List<Long> = BatchQueueRuntimeStore.state.value.tasks.map { it.id }

    @Test fun queuedTasksMoveUpAndDownWithoutChangingMembership() {
        val a = add(1)
        val b = add(2)
        val c = add(3)
        assertTrue(BatchQueueRuntimeStore.moveTaskUp(c).isSuccess)
        assertEquals(listOf(a, c, b), ids())
        assertTrue(BatchQueueRuntimeStore.moveTaskDown(a).isSuccess)
        assertEquals(listOf(c, a, b), ids())
        assertEquals(3, ids().toSet().size)
    }

    @Test fun edgeMoveFailsWithoutChangingState() {
        val first = add(1)
        val last = add(2)
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(BatchQueueRuntimeStore.moveTaskUp(first).isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertTrue(BatchQueueRuntimeStore.moveTaskDown(last).isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun unknownTaskFailsWithoutChangingState() {
        add(1)
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(BatchQueueRuntimeStore.moveTaskDown(Long.MAX_VALUE).isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun runningTaskCannotBeMoved() {
        val first = add(1)
        add(2)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(first), 1))
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(BatchQueueRuntimeStore.moveTaskDown(first).isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun reorderingSkipsTerminalTasksAndPreservesTheirOutcomes() {
        val first = add(1)
        val middle = add(2)
        val last = add(3)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(middle), 1))
        BatchQueueRuntimeStore.markTaskSuccess(middle, "/done.pdf", 12L)
        BatchQueueRuntimeStore.finishProcessing("Done")
        assertTrue(BatchQueueRuntimeStore.moveTaskDown(first).isSuccess)
        assertEquals(listOf(last, middle, first), ids())
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.SUCCESS, state.tasks[1].status)
        assertEquals("/done.pdf", state.tasks[1].outputPath)
        assertEquals(1, state.processedCount)
        assertEquals(1, state.successCount)
        assertEquals(0, state.failureCount)
    }

    @Test fun terminalTaskCannotBeReordered() {
        val first = add(1)
        val second = add(2)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(second), 1))
        BatchQueueRuntimeStore.markTaskFailure(second, "failed")
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(BatchQueueRuntimeStore.moveTaskUp(second).isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
        assertEquals(listOf(first, second), ids())
    }

    @Test fun concurrentEnqueuesAreNeverLostDuringReorders() {
        val first = add(1)
        val second = add(2)
        val pool = Executors.newFixedThreadPool(3)
        try {
            val gate = CountDownLatch(1)
            val writer = pool.submit {
                gate.await()
                repeat(100) { index -> add(1000 + index) }
            }
            val reorderA = pool.submit {
                gate.await()
                repeat(400) { BatchQueueRuntimeStore.moveTaskDown(first) }
            }
            val reorderB = pool.submit {
                gate.await()
                repeat(400) { BatchQueueRuntimeStore.moveTaskUp(second) }
            }
            gate.countDown()
            listOf(writer, reorderA, reorderB).forEach { it.get(20, TimeUnit.SECONDS) }
            val all = ids()
            assertEquals(102, all.size)
            assertEquals(102, all.toSet().size)
            assertTrue(all.containsAll(listOf(first, second)))
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun concurrentTerminalCompletionSurvivesQueuedReorders() {
        val first = add(1)
        val running = add(2)
        val last = add(3)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(running), 1))
        val pool = Executors.newFixedThreadPool(3)
        try {
            val gate = CountDownLatch(1)
            val terminal = pool.submit {
                gate.await()
                BatchQueueRuntimeStore.markTaskSuccess(running, "/done.pdf", 99L)
            }
            val reorderA = pool.submit {
                gate.await()
                repeat(500) { BatchQueueRuntimeStore.moveTaskDown(first) }
            }
            val reorderB = pool.submit {
                gate.await()
                repeat(500) { BatchQueueRuntimeStore.moveTaskUp(last) }
            }
            gate.countDown()
            listOf(terminal, reorderA, reorderB).forEach { it.get(20, TimeUnit.SECONDS) }
            val state = BatchQueueRuntimeStore.state.value
            assertEquals(setOf(first, running, last), ids().toSet())
            val finished = state.tasks.single { it.id == running }
            assertEquals(BatchTaskStatus.SUCCESS, finished.status)
            assertEquals("/done.pdf", finished.outputPath)
            assertEquals(99L, finished.outputSizeBytes)
            assertEquals(1, state.processedCount)
            assertEquals(1, state.successCount)
            assertEquals(0, state.failureCount)
            assertFalse(state.tasks.any { it.id == running && it.status == BatchTaskStatus.QUEUED })
        } finally {
            pool.shutdownNow()
        }
    }
}
