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
class BatchQueuePresetReplayRaceTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun enqueue(index: Int): Long {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/replay-$index.pdf")),
            emptyList()
        ).isSuccess)
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    private fun preset(index: Int) = BatchQueuePresetTask(
        type = BatchTaskType.PDF_COMPRESS,
        inputUris = listOf("content://provider/preset-$index.pdf"),
        inputSummary = "preset.pdf",
        outputBaseName = "preset_output"
    )

    @Test fun replayWhileProcessingFailsWithoutChangingActiveQueue() {
        val originalId = enqueue(1)
        assertEquals(listOf(originalId), runBlocking {
            BatchQueueRuntimeStore.beginProcessing()
        }.getOrThrow())
        val before = BatchQueueRuntimeStore.state.value

        val result = BatchQueueRuntimeStore.replaceQueueWithPreset("new", listOf(preset(1)))
        assertTrue(result.isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun validReplayAfterProcessingCompletesReplacesQueue() {
        enqueue(2)
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
        BatchQueueRuntimeStore.finishProcessing("Done")

        assertTrue(BatchQueueRuntimeStore.replaceQueueWithPreset("saved", listOf(preset(2))).isSuccess)
        val state = BatchQueueRuntimeStore.state.value
        assertFalse(state.isProcessing)
        assertEquals(1, state.tasks.size)
        assertEquals("preset_output", state.tasks.single().outputBaseName)
        assertEquals("content://provider/preset-2.pdf", state.tasks.single().inputUris.single().toString())
        assertEquals(0, state.processedCount)
    }

    @Test fun invalidReplayLeavesExistingQueueUntouched() {
        enqueue(3)
        val before = BatchQueueRuntimeStore.state.value
        val invalid = preset(3).copy(inputUris = listOf("file:///tmp/not-content.pdf"))
        assertTrue(BatchQueueRuntimeStore.replaceQueueWithPreset("bad", listOf(invalid)).isFailure)
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun concurrentReplayAndBeginNeverDiscardStartedTaskIds() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(150) { index ->
                reset()
                val originalId = enqueue(1000 + index)
                val gate = CountDownLatch(1)
                val begin = pool.submit<Result<List<Long>>> {
                    gate.await()
                    runBlocking { BatchQueueRuntimeStore.beginProcessing() }
                }
                val replay = pool.submit<Result<Unit>> {
                    gate.await()
                    BatchQueueRuntimeStore.replaceQueueWithPreset("saved", listOf(preset(index)))
                }
                gate.countDown()
                val started = begin.get(10, TimeUnit.SECONDS)
                val replaced = replay.get(10, TimeUnit.SECONDS)
                assertTrue(started.isSuccess)
                val state = BatchQueueRuntimeStore.state.value
                assertTrue(state.isProcessing)
                assertEquals(started.getOrThrow(), state.tasks.map { it.id })
                if (replaced.isSuccess) {
                    assertTrue(state.tasks.none { it.id == originalId })
                    assertEquals("preset_output", state.tasks.single().outputBaseName)
                } else {
                    assertEquals(listOf(originalId), state.tasks.map { it.id })
                }
            }
        } finally {
            pool.shutdownNow()
        }
    }
}
