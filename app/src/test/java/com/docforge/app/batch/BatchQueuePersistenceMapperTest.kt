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
    fun snapshot_persistsOnlyRecoverableTasks_inQueueOrder() {
        val queued = task(11L, BatchTaskStatus.QUEUED)
        val running = task(12L, BatchTaskStatus.RUNNING)
        val completed = task(13L, BatchTaskStatus.SUCCESS)

        val entities = BatchQueuePersistenceMapper.toEntities(
            tasks = listOf(queued, running, completed),
            snapshotAtMillis = 1_000L
        )

        assertEquals(listOf(11L, 12L), entities.map { it.id })
        assertEquals(listOf(1_000L, 1_001L), entities.map { it.createdAtMillis })
        assertEquals(listOf("QUEUED", "RUNNING"), entities.map { it.status })
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
    fun nextTaskIdAfterRestore_advancesPastRecoveredIds_withoutMovingBackward() {
        assertEquals(43L, nextTaskIdAfterRestore(currentNextId = 5L, restoredMaxId = 42L))
        assertEquals(100L, nextTaskIdAfterRestore(currentNextId = 100L, restoredMaxId = 42L))
        assertEquals(Long.MAX_VALUE, nextTaskIdAfterRestore(currentNextId = 7L, restoredMaxId = Long.MAX_VALUE))
    }

    @Test
    fun restore_rejectsMalformedTypeStatusUriAndInputCount() {
        val malformed = listOf(
            entity(id = 1L, taskType = "UNKNOWN"),
            entity(id = 2L, status = "SUCCESS"),
            entity(id = 3L, inputUrisJson = "[\"file:///tmp/a.pdf\"]"),
            entity(
                id = 4L,
                taskType = BatchTaskType.PDF_MERGE.name,
                inputUrisJson = "[\"content://provider/only-one.pdf\"]"
            ),
            entity(id = Long.MAX_VALUE),
            entity(id = 6L, outputBaseName = "../unsafe")
        )

        assertTrue(BatchQueuePersistenceMapper.fromEntities(malformed).isEmpty())
    }

    private fun task(id: Long, status: BatchTaskStatus) = BatchQueueTask(
        id = id,
        type = BatchTaskType.PDF_COMPRESS,
        inputUris = listOf(Uri.parse("content://provider/$id.pdf")),
        inputSummary = "input-$id.pdf",
        outputBaseName = "output_$id",
        status = status
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
