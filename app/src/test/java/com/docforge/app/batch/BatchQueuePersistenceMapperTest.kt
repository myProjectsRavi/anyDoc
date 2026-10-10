package com.docforge.app.batch

import android.net.Uri
import com.docforge.core.storage.db.BatchQueueTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueuePersistenceMapperTest {

    @Test
    fun snapshot_persistsTerminalEvidence_inQueueOrder() {
        val queued = task(11L, BatchTaskStatus.QUEUED)
        val running = task(12L, BatchTaskStatus.RUNNING)
        val completed = task(
            id = 13L,
            status = BatchTaskStatus.SUCCESS,
            outputPath = "/documents/output_13.pdf",
            outputSizeBytes = 4_096L
        )
        val failed = task(
            id = 14L,
            status = BatchTaskStatus.FAILED,
            errorMessage = "conversion failed"
        )

        val entities = BatchQueuePersistenceMapper.toEntities(
            tasks = listOf(queued, running, completed, failed),
            snapshotAtMillis = 1_000L
        )

        assertEquals(listOf(11L, 12L, 13L, 14L), entities.map { it.id })
        assertEquals(listOf(1_000L, 1_001L, 1_002L, 1_003L), entities.map { it.createdAtMillis })
        assertEquals(listOf("QUEUED", "RUNNING", "SUCCESS", "FAILED"), entities.map { it.status })
        assertEquals("/documents/output_13.pdf", entities[2].outputPath)
        assertEquals(4_096L, entities[2].outputSizeBytes)
        assertEquals("conversion failed", entities[3].errorMessage)
    }

    @Test
    fun restore_convertsRunningToQueued_withoutAutoExecutionState() {
        val restored = BatchQueuePersistenceMapper.fromEntities(
            listOf(entity(id = 42L, status = "RUNNING"))
        ).single()

        assertEquals(BatchTaskStatus.QUEUED, restored.status)
        assertNull(restored.outputPath)
        assertNull(restored.outputSizeBytes)
        assertTrue(restored.errorMessage.orEmpty().contains("Recovered after app restart"))
    }

    @Test
    fun restore_skipsTerminalRows_butKeepsRecoverableWork() {
        val restored = BatchQueuePersistenceMapper.fromEntities(
            listOf(
                entity(id = 51L, status = "SUCCESS"),
                entity(id = 52L, status = "FAILED"),
                entity(id = 53L, status = "CANCELED"),
                entity(id = 54L, status = "QUEUED")
            )
        )

        assertEquals(listOf(54L), restored.map { it.id })
        assertEquals(BatchTaskStatus.QUEUED, restored.single().status)
    }

    @Test
    fun nextTaskIdAfterRestore_advancesPastRecoveredIds_withoutMovingBackward() {
        assertEquals(43L, nextTaskIdAfterRestore(currentNextId = 5L, restoredMaxId = 42L))
        assertEquals(100L, nextTaskIdAfterRestore(currentNextId = 100L, restoredMaxId = 42L))
        assertEquals(Long.MAX_VALUE, nextTaskIdAfterRestore(currentNextId = 7L, restoredMaxId = Long.MAX_VALUE))
    }

    @Test
    fun restore_rejectsMalformedTypeStatusUriAndInputCount() {
        val malformed = listOf(
            entity(id = 1L, taskType = "UNKNOWN"),
            entity(id = 2L, status = "UNKNOWN"),
            entity(id = 3L, inputUrisJson = "[\"file:///tmp/a.pdf\"]"),
            entity(
                id = 4L,
                taskType = BatchTaskType.PDF_MERGE.name,
                inputUrisJson = "[\"content://provider/only-one.pdf\"]"
            ),
            entity(id = Long.MAX_VALUE),
            entity(id = 6L, outputBaseName = "../unsafe"),
            entity(id = 7L, inputUrisJson = "not-json"),
            entity(id = 8L, inputUrisJson = "[]"),
            entity(id = 9L, status = "RUNNING", inputUrisJson = "not-json"),
            entity(id = 10L, status = "RUNNING", taskType = "UNKNOWN"),
            entity(id = 11L, inputUrisJson = "[\"\"]"),
            entity(id = 12L, inputUrisJson = "{}"),
            entity(id = 0L)
        )

        malformed.forEach { row ->
            try {
                BatchQueuePersistenceMapper.fromEntities(listOf(row))
                org.junit.Assert.fail("Expected corrupt saved task #${row.id} to fail recovery")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("Saved batch task"))
            }
        }
    }

    @Test
    fun restore_rejectsWholeSnapshotWhenAnyRecoverableRowIsCorrupt() {
        val rows = listOf(
            entity(id = 101L),
            entity(id = 102L, inputUrisJson = "[\"file:///unsafe.pdf\"]"),
            entity(id = 103L)
        )
        try {
            BatchQueuePersistenceMapper.fromEntities(rows)
            org.junit.Assert.fail("Expected failure rather than a partial recovered queue")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("#102"))
        }
    }

    @Test
    fun restore_stillSkipsTerminalRowsEvenWhenTheirPayloadIsMalformed() {
        val restored = BatchQueuePersistenceMapper.fromEntities(
            listOf(
                entity(id = Long.MAX_VALUE, status = "SUCCESS", inputUrisJson = "not-json"),
                entity(id = 202L, status = "FAILED", outputBaseName = "../invalid"),
                entity(id = 203L, status = "CANCELED", taskType = "UNKNOWN"),
                entity(id = 204L, status = "QUEUED")
            )
        )
        assertEquals(listOf(204L), restored.map { it.id })
    }

    private fun task(
        id: Long,
        status: BatchTaskStatus,
        outputPath: String? = null,
        outputSizeBytes: Long? = null,
        errorMessage: String? = null
    ) = BatchQueueTask(
        id = id,
        type = BatchTaskType.PDF_COMPRESS,
        inputUris = listOf(Uri.parse("content://provider/$id.pdf")),
        inputSummary = "input-$id.pdf",
        outputBaseName = "output_$id",
        status = status,
        outputPath = outputPath,
        outputSizeBytes = outputSizeBytes,
        errorMessage = errorMessage
    )

    private fun entity(
        id: Long,
        taskType: String = BatchTaskType.PDF_COMPRESS.name,
        status: String = "QUEUED",
        inputUrisJson: String = "[\"content://provider/input.pdf\"]",
        outputBaseName: String = "output_$id"
    ) = BatchQueueTaskEntity(
        id = id,
        taskType = taskType,
        inputUrisJson = inputUrisJson,
        inputSummary = "input.pdf",
        outputBaseName = outputBaseName,
        status = status,
        createdAtMillis = id
    )
}
