package com.docforge.app.batch

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

        convertWithBestEffortHistory(
            convert = { "/output/converted.pdf" to 42L },
            recordHistory = { throw IllegalStateException("history unavailable") },
            onPublished = { published ->
                BatchQueueRuntimeStore.markTaskSuccess(task.id, published.first, published.second, run.runId)
            }
        )

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

    @Test fun canceledHistoryPreservesPublishedSuccessAndCancelsPendingWork() = runBlocking {
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/first.pdf")),
            emptyList()
        ).getOrThrow()
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/second.pdf")),
            emptyList()
        ).getOrThrow()
        val run = BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()
        val task = BatchQueueRuntimeStore.startNextQueuedTask(run.taskIds.toSet(), 2, run.runId)!!
        try {
            convertWithBestEffortHistory(
                convert = { "/output/converted.pdf" to 42L },
                recordHistory = { _: Pair<String, Long> -> throw CancellationException("history canceled") },
                onPublished = { published ->
                    BatchQueueRuntimeStore.markTaskSuccess(
                        task.id, published.first, published.second, run.runId
                    )
                }
            )
            fail("Expected history cancellation")
        } catch (expected: CancellationException) {
            assertEquals("history canceled", expected.message)
            BatchQueueRuntimeStore.markTaskCanceled(task.id, run.runId)
            assertTrue(BatchQueueRuntimeStore.cancelPendingRun(run.taskIds.toSet(), run.runId))
        }
        val state = BatchQueueRuntimeStore.state.value
        assertFalse(state.isProcessing)
        assertEquals(BatchTaskStatus.SUCCESS, state.tasks[0].status)
        assertEquals("/output/converted.pdf", state.tasks[0].outputPath)
        assertEquals(42L, state.tasks[0].outputSizeBytes)
        assertEquals(BatchTaskStatus.CANCELED, state.tasks[1].status)
        assertEquals(1, state.processedCount)
        assertEquals(1, state.successCount)
        assertEquals(0, state.failureCount)
    }

    @Test fun cancellationBeforeConversionResultDoesNotMarkSuccess() = runBlocking {
        BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/input.pdf")),
            emptyList()
        ).getOrThrow()
        val run = BatchQueueRuntimeStore.beginProcessingSession().getOrThrow()
        val task = BatchQueueRuntimeStore.startNextQueuedTask(run.taskIds.toSet(), 1, run.runId)!!
        try {
            convertWithBestEffortHistory(
                convert = { throw CancellationException("converter canceled") },
                recordHistory = { _: Pair<String, Long> -> fail("History must not run") },
                onPublished = { published: Pair<String, Long> ->
                    BatchQueueRuntimeStore.markTaskSuccess(
                        task.id, published.first, published.second, run.runId
                    )
                }
            )
            fail("Expected conversion cancellation")
        } catch (expected: CancellationException) {
            BatchQueueRuntimeStore.markTaskCanceled(task.id, run.runId)
            assertTrue(BatchQueueRuntimeStore.cancelPendingRun(run.taskIds.toSet(), run.runId))
        }
        val state = BatchQueueRuntimeStore.state.value
        assertEquals(BatchTaskStatus.CANCELED, state.tasks.single().status)
        assertEquals(null, state.tasks.single().outputPath)
        assertEquals(1, state.processedCount)
        assertEquals(0, state.successCount)
        assertEquals(1, state.failureCount)
    }
}
