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
class BatchQueueTaskMutationTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun add(index: Int): Long {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/mutation-$index.pdf")),
            emptyList()
        ).isSuccess)
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    @Test fun queuedTaskCanBeRemovedWithoutAffectingOtherTasks() {
        val first = add(1)
        val second = add(2)
        assertTrue(BatchQueueRuntimeStore.removeTask(first).isSuccess)
        assertEquals(listOf(second), BatchQueueRuntimeStore.state.value.tasks.map { it.id })
    }

    @Test fun unknownRemovalAndRenameLeaveStateUnchanged() {
        add(1)
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(BatchQueueRuntimeStore.removeTask(Long.MAX_VALUE).isFailure)
        assertTrue(BatchQueueRuntimeStore.updateOutputBaseName(Long.MAX_VALUE, "renamed").isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun runningTaskCannotBeRemovedOrRenamed() {
        val id = add(1)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1))
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        val before = BatchQueueRuntimeStore.state.value
        assertTrue(BatchQueueRuntimeStore.removeTask(id).isFailure)
        assertTrue(BatchQueueRuntimeStore.updateOutputBaseName(id, "renamed").isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun terminalTaskCanBeRemovedButNotRenamed() {
        val id = add(1)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1))
        BatchQueueRuntimeStore.markTaskSuccess(id, "/done.pdf", 10L)
        assertTrue(BatchQueueRuntimeStore.updateOutputBaseName(id, "renamed").isFailure)
        assertTrue(BatchQueueRuntimeStore.removeTask(id).isSuccess)
        assertTrue(BatchQueueRuntimeStore.state.value.tasks.isEmpty())
        assertEquals(1, BatchQueueRuntimeStore.state.value.processedCount)
    }

    @Test fun queuedRenameSanitizesNameWithoutChangingOtherTasks() {
        val first = add(1)
        val second = add(2)
        val original = BatchQueueRuntimeStore.state.value.tasks.single { it.id == second }.outputBaseName
        assertTrue(BatchQueueRuntimeStore.updateOutputBaseName(first, " invoice 2026.pdf ").isSuccess)
        val tasks = BatchQueueRuntimeStore.state.value.tasks
        assertEquals("invoice_2026_pdf", tasks.single { it.id == first }.outputBaseName)
        assertEquals(original, tasks.single { it.id == second }.outputBaseName)
    }

    @Test fun concurrentStartAndRemovalPreserveRunningTask() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(100) { index ->
                reset()
                val id = add(1000 + index)
                val gate = CountDownLatch(1)
                val start = pool.submit {
                    gate.await()
                    BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1)
                }
                val remove = pool.submit {
                    gate.await()
                    BatchQueueRuntimeStore.removeTask(id)
                }
                gate.countDown()
                start.get(10, TimeUnit.SECONDS)
                val result = remove.get(10, TimeUnit.SECONDS)
                val current = BatchQueueRuntimeStore.state.value.tasks.singleOrNull { it.id == id }
                if (result.isSuccess) {
                    assertTrue(current == null)
                } else {
                    assertNotNull(current)
                    assertEquals(BatchTaskStatus.RUNNING, current!!.status)
                }
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun concurrentStartAndRenameNeverRenameAfterRunning() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(100) { index ->
                reset()
                val id = add(2000 + index)
                val original = BatchQueueRuntimeStore.state.value.tasks.single().outputBaseName
                val gate = CountDownLatch(1)
                val start = pool.submit {
                    gate.await()
                    BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1)
                }
                val rename = pool.submit {
                    gate.await()
                    BatchQueueRuntimeStore.updateOutputBaseName(id, "new_name")
                }
                gate.countDown()
                start.get(10, TimeUnit.SECONDS)
                val result = rename.get(10, TimeUnit.SECONDS)
                val task = BatchQueueRuntimeStore.state.value.tasks.single()
                assertEquals(BatchTaskStatus.RUNNING, task.status)
                assertEquals(if (result.isSuccess) "new_name" else original, task.outputBaseName)
            }
        } finally {
            pool.shutdownNow()
        }
    }
}
