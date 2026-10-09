package com.docforge.app.batch

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueRecoveryGateTest {
    private val task = BatchQueueTask(
        id = 71L,
        type = BatchTaskType.PDF_COMPRESS,
        inputUris = listOf(Uri.parse("content://provider/71.pdf")),
        inputSummary = "71.pdf",
        outputBaseName = "output_71"
    )

    @Test fun failedRecoveryDoesNotWriteInitialEmptySnapshot() = runBlocking {
        var writes = 0
        var restores = 0
        val errors = mutableListOf<String>()
        collectBatchQueueAfterRecovery(
            recover = { throw IllegalStateException("read failure") },
            restore = { restores++ },
            states = flow { throw AssertionError("Must not collect after recovery failure") },
            onRecoveryFailure = { errors += it.message.orEmpty() },
            onState = { writes++ }
        )
        assertEquals(0, writes)
        assertEquals(0, restores)
        assertEquals(listOf("read failure"), errors)
    }

    @Test fun recoveryRestoresBeforeFirstWrite() = runBlocking {
        val events = mutableListOf<String>()
        collectBatchQueueAfterRecovery(
            recover = { events += "read"; listOf(task) },
            restore = { assertEquals(listOf(task), it); events += "restore" },
            states = flowOf(BatchQueueUiState(tasks = listOf(task))),
            onRecoveryFailure = { fail("Unexpected failure: $it") },
            onState = { assertEquals(listOf(task), it.tasks); events += "write" }
        )
        assertEquals(listOf("read", "restore", "write"), events)
    }

    @Test fun successfulEmptyRecoveryAllowsEmptySnapshot() = runBlocking {
        var writes = 0
        collectBatchQueueAfterRecovery(
            recover = { emptyList() },
            restore = { assertTrue(it.isEmpty()) },
            states = flowOf(BatchQueueUiState()),
            onRecoveryFailure = { fail("Unexpected failure: $it") },
            onState = { writes++ }
        )
        assertEquals(1, writes)
    }

    @Test fun cancellationDoesNotReportFailureOrWrite() = runBlocking {
        var writes = 0
        var errors = 0
        try {
            collectBatchQueueAfterRecovery(
                recover = { throw CancellationException("cancelled") },
                restore = { fail("Must not restore") },
                states = flowOf(BatchQueueUiState()),
                onRecoveryFailure = { errors++ },
                onState = { writes++ }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
        assertEquals(0, errors)
        assertEquals(0, writes)
    }

    @Test fun cancellationDuringStateCollectionPropagates() = runBlocking {
        var errors = 0
        try {
            collectBatchQueueAfterRecovery(
                recover = { emptyList() },
                restore = {},
                states = flow { throw CancellationException("collect cancelled") },
                onRecoveryFailure = { errors++ },
                onState = { fail("Must not write") }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("collect cancelled", expected.message)
        }
        assertEquals(0, errors)
    }

    @Test fun cancellationDuringStateWritePropagates() = runBlocking {
        var errors = 0
        try {
            collectBatchQueueAfterRecovery(
                recover = { emptyList() },
                restore = {},
                states = flowOf(BatchQueueUiState()),
                onRecoveryFailure = { errors++ },
                onState = { throw CancellationException("write cancelled") }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("write cancelled", expected.message)
        }
        assertEquals(0, errors)
    }

    @Test fun failedRecoveryCanRetryWithoutLosingStoredTasks() = runBlocking {
        val events = mutableListOf<String>()
        collectBatchQueueAfterRecovery(
            recover = { throw IllegalStateException("temporary read failure") },
            restore = { events += "unexpected restore" },
            states = flowOf(BatchQueueUiState()),
            onRecoveryFailure = { events += "failure" },
            onState = { events += "unexpected write" }
        )
        assertEquals(listOf("failure"), events)
        collectBatchQueueAfterRecovery(
            recover = { listOf(task) },
            restore = { assertEquals(listOf(task), it); events += "restore" },
            states = flowOf(BatchQueueUiState(tasks = listOf(task))),
            onRecoveryFailure = { fail("Unexpected retry failure: $it") },
            onState = { events += "write" }
        )
        assertEquals(listOf("failure", "restore", "write"), events)
    }

    @Test fun successfulRecoveryContinuesObservingLaterQueueChanges() = runBlocking {
        val persisted = mutableListOf<List<BatchQueueTask>>()
        collectBatchQueueAfterRecovery(
            recover = { listOf(task) },
            restore = { assertEquals(listOf(task), it) },
            states = flowOf(
                BatchQueueUiState(tasks = listOf(task)),
                BatchQueueUiState(tasks = emptyList())
            ),
            onRecoveryFailure = { fail("Unexpected failure: $it") },
            onState = { persisted += it.tasks }
        )
        assertEquals(listOf(listOf(task), emptyList<BatchQueueTask>()), persisted)
    }

    @Test fun corruptRecoverableRowAbortsRecoveryBeforeAnySnapshotWrite() = runBlocking {
        val valid = BatchQueuePersistenceMapper.toEntities(listOf(task)).single()
        val corrupt = valid.copy(id = 72L, inputUrisJson = "not-json")
        var restores = 0
        var writes = 0
        val errors = mutableListOf<String>()

        collectBatchQueueAfterRecovery(
            recover = { BatchQueuePersistenceMapper.fromEntities(listOf(valid, corrupt)) },
            restore = { restores++ },
            states = flowOf(BatchQueueUiState()),
            onRecoveryFailure = { errors += it.message.orEmpty() },
            onState = { writes++ }
        )

        assertEquals(0, restores)
        assertEquals(0, writes)
        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("#72"))
    }
}
