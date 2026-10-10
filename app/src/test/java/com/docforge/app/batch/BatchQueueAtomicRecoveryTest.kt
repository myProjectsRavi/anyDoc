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
class BatchQueueAtomicRecoveryTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun recovered(index: Int): BatchQueueTask = BatchQueueTask(
        id = 10_000_000_000L + index,
        type = BatchTaskType.PDF_COMPRESS,
        inputUris = listOf(Uri.parse("content://provider/recovered-$index.pdf")),
        inputSummary = "recovered-$index.pdf",
        outputBaseName = "recovered_$index"
    )

    private fun enqueue(index: Int): Result<Unit> = BatchQueueRuntimeStore.addTask(
        BatchTaskType.PDF_COMPRESS,
        listOf(Uri.parse("content://provider/added-$index.pdf")),
        emptyList()
    )

    private fun preset(index: Int): BatchQueuePresetTask = BatchQueuePresetTask(
        type = BatchTaskType.PDF_COMPRESS,
        inputUris = listOf("content://provider/preset-$index.pdf"),
        inputSummary = "preset-$index.pdf",
        outputBaseName = "preset_$index"
    )

    @Test fun emptyAndNonRecoverableInputDoesNotChangeQueue() {
        val before = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(emptyList()))
        assertFalse(BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(
            listOf(recovered(1).copy(status = BatchTaskStatus.SUCCESS))
        ))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun existingQueueIsNeverOverwrittenByRecovery() {
        assertTrue(enqueue(1).isSuccess)
        val before = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(listOf(recovered(2))))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun recoveredIdsRemainReservedForSubsequentEnqueuesAndPresets() {
        val recoveredTask = recovered(100)
        assertTrue(BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(listOf(recoveredTask)))
        assertTrue(enqueue(3).isSuccess)
        val afterAdd = BatchQueueRuntimeStore.state.value.tasks
        assertEquals(2, afterAdd.size)
        assertTrue(afterAdd.last().id > recoveredTask.id)
        assertEquals(2, afterAdd.map { it.id }.toSet().size)

        assertTrue(BatchQueueRuntimeStore.replaceQueueWithPreset("saved", listOf(preset(3), preset(4))).isSuccess)
        val afterPreset = BatchQueueRuntimeStore.state.value.tasks
        assertEquals(2, afterPreset.size)
        assertEquals(2, afterPreset.map { it.id }.toSet().size)
        assertTrue(afterPreset.all { it.id > recoveredTask.id })
    }

    @Test fun recoveryCannotReplaceProcessingQueue() {
        assertTrue(enqueue(5).isSuccess)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        val before = BatchQueueRuntimeStore.state.value
        assertFalse(BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(listOf(recovered(6))))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun concurrentRecoveryAndEnqueueNeverLoseAddedTaskOrReuseId() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(150) { index ->
                reset()
                val candidate = recovered(1000 + index)
                val gate = CountDownLatch(1)
                val restoration = pool.submit<Boolean> {
                    gate.await()
                    BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(listOf(candidate))
                }
                val addition = pool.submit<Result<Unit>> {
                    gate.await()
                    enqueue(1000 + index)
                }
                gate.countDown()
                val didRestore = restoration.get(10, TimeUnit.SECONDS)
                assertTrue(addition.get(10, TimeUnit.SECONDS).isSuccess)
                val tasks = BatchQueueRuntimeStore.state.value.tasks
                assertEquals(if (didRestore) 2 else 1, tasks.size)
                assertEquals(tasks.size, tasks.map { it.id }.toSet().size)
                assertTrue(tasks.any { it.inputUris.single().toString().contains("added-") })
                assertEquals(didRestore, tasks.any { it.id == candidate.id })
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun concurrentRecoveryAndPresetReplayMaintainUniquePublishedIds() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(150) { index ->
                reset()
                val candidate = recovered(2000 + index)
                val gate = CountDownLatch(1)
                val restoration = pool.submit<Boolean> {
                    gate.await()
                    BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(listOf(candidate))
                }
                val replay = pool.submit<Result<Unit>> {
                    gate.await()
                    BatchQueueRuntimeStore.replaceQueueWithPreset("saved", listOf(preset(index), preset(index + 10000)))
                }
                gate.countDown()
                restoration.get(10, TimeUnit.SECONDS)
                assertTrue(replay.get(10, TimeUnit.SECONDS).isSuccess)
                val tasks = BatchQueueRuntimeStore.state.value.tasks
                assertEquals(2, tasks.size)
                assertEquals(2, tasks.map { it.id }.toSet().size)
                assertTrue(tasks.all { it.inputUris.single().toString().contains("preset-") })
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun concurrentRecoveryAndBeginPreserveStartedTasks() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(150) { index ->
                reset()
                val candidate = recovered(3000 + index)
                val gate = CountDownLatch(1)
                val restoration = pool.submit<Boolean> {
                    gate.await()
                    BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(listOf(candidate))
                }
                val begin = pool.submit<Result<List<Long>>> {
                    gate.await()
                    runBlocking { BatchQueueRuntimeStore.beginProcessing() }
                }
                gate.countDown()
                assertTrue(restoration.get(10, TimeUnit.SECONDS))
                val started = begin.get(10, TimeUnit.SECONDS)
                val state = BatchQueueRuntimeStore.state.value
                assertEquals(listOf(candidate.id), state.tasks.map { it.id })
                if (started.isSuccess) {
                    assertEquals(listOf(candidate.id), started.getOrThrow())
                    assertTrue(state.isProcessing)
                } else {
                    assertFalse(state.isProcessing)
                }
            }
        } finally {
            pool.shutdownNow()
        }
    }
}
