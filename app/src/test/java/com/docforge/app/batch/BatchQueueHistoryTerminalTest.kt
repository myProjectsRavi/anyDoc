package com.docforge.app.batch

import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueHistoryTerminalTest {
    @Before fun setup() = reset()
    @After fun cleanup() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("reset")
        BatchQueueRuntimeStore.clearQueue().getOrThrow()
    }

    @Test fun historyFailurePreservesSuccessTerminalStateAndCounters() = runBlocking {
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/input.pdf")),
            emptyList()
        ).getOrThrow()
        val run = BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()
        val task = BatchQueueRuntimeStore.startNextQueuedTask(
            run.taskIds.toSet(), 1, run.runId
        )!!

        val published = convertWithBestEffortHistory(
            convert = { "/output/converted.pdf" to 42L },
            recordHistory = { throw IllegalStateException("history unavailable") }
        )
        BatchQueueRuntimeStore.markTaskSuccess(task.id, published.first, published.second, run.runId)

        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.SUCCESS, state.tasks.single().status)
        assertEquals("/output/converted.pdf", state.tasks.single().outputPath)
        assertEquals(42L, state.tasks.single().outputSizeBytes)
        assertEquals(1, state.processedCount)
        assertEquals(1, state.successCount)
        assertEquals(0, state.failureCount)
    }

    @Test fun conversionFailureStillTransitionsToFailure() = runBlocking {
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/input.pdf")),
            emptyList()
        ).getOrThrow()
        val run = BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()
        val task = BatchQueueRuntimeStore.startNextQueuedTask(
            run.taskIds.toSet(), 1, run.runId
        )!!
        var historyInvoked = false
        try {
            convertWithBestEffortHistory(
                convert = { throw IllegalStateException("conversion failed") },
                recordHistory = { _: String -> historyInvoked = true }
            )
        } catch (error: IllegalStateException) {
            BatchQueueRuntimeStore.markTaskFailure(task.id, error.message ?: "failed", run.runId)
        }
        assertFalse(historyInvoked)
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.FAILED, state.tasks.single().status)
        assertEquals(null, state.tasks.single().outputPath)
        assertEquals(1, state.processedCount)
        assertEquals(0, state.successCount)
        assertEquals(1, state.failureCount)
    }
}
