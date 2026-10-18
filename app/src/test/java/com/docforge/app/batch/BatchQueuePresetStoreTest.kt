package com.docforge.app.batch

import com.docforge.core.storage.db.BatchPresetDao
import com.docforge.core.storage.db.BatchPresetEntity
import kotlinx.coroutines.CancellationException
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
class BatchQueuePresetStoreTest {
    private val validTask = """[{"type":"PDF_COMPRESS","inputUris":["content://files/a.pdf"],"inputSummary":"a.pdf","outputBaseName":"compressed"}]"""

    @Test fun validPresetsRemainReadable() = runBlocking {
        val store = BatchQueuePresetStore(FakeDao(mutableListOf(row(1, validTask))))
        val preset = store.readPresets().single()
        assertEquals(1L, preset.id)
        assertEquals(BatchTaskType.PDF_COMPRESS, preset.tasks.single().type)
        assertEquals(listOf("content://files/a.pdf"), preset.tasks.single().inputUris)
    }

    @Test fun emptyDatabaseIsLegitimatelyEmpty() = runBlocking {
        assertTrue(BatchQueuePresetStore(FakeDao()).readPresets().isEmpty())
    }

    @Test fun malformedJsonIsNotReportedAsEmpty() = runBlocking {
        assertUnreadable("{")
    }

    @Test fun emptyTaskArrayIsNotSilentlyDropped() = runBlocking {
        assertUnreadable("[]")
    }

    @Test fun unknownTaskTypeIsNotSilentlyDropped() = runBlocking {
        assertUnreadable(validTask.replace("PDF_COMPRESS", "NOT_A_TASK"))
    }

    @Test fun invalidTaskObjectIsNotSilentlySkipped() = runBlocking {
        assertUnreadable("""[null]""")
    }

    @Test fun missingUrisAreNotSilentlyDefaulted() = runBlocking {
        assertUnreadable(validTask.replace("inputUris", "missingUris"))
    }

    @Test fun blankUriIsNotSilentlyDropped() = runBlocking {
        assertUnreadable(validTask.replace("content://files/a.pdf", "  "))
    }

    @Test fun malformedSecondRowDoesNotReturnPartialList() = runBlocking {
        val dao = FakeDao(mutableListOf(row(1, validTask), row(2, "{")))
        val result = loadBatchPresets { BatchQueuePresetStore(dao).readPresets() }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("#2"))
    }

    @Test fun databaseReadFailureIsVisible() = runBlocking {
        val dao = FakeDao().apply { readFailure = IllegalStateException("database unavailable") }
        val result = loadBatchPresets { BatchQueuePresetStore(dao).readPresets() }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("database unavailable"))
    }

    @Test fun failedRefreshDoesNotReplaceLastGoodPresets() = runBlocking {
        val previous = listOf(BatchQueuePreset(10, "previous", 100, listOf(
            BatchQueuePresetTask(BatchTaskType.PDF_COMPRESS, listOf("content://files/a.pdf"), "a.pdf", "out")
        )))
        var displayed = previous
        val result = loadBatchPresets { throw IllegalStateException("corrupt database") }
        result.onSuccess { displayed = it }
        assertTrue(result.isFailure)
        assertEquals(previous, displayed)
    }

    @Test fun cancelledRefreshIsNotConvertedToStorageFailure() = runBlocking {
        try {
            loadBatchPresets { throw CancellationException("cancelled") }
            fail("Cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test fun savingAndDeletingValidPresetStillWorks() = runBlocking {
        val dao = FakeDao()
        val store = BatchQueuePresetStore(dao)
        val task = BatchQueuePresetTask(BatchTaskType.PDF_COMPRESS, listOf("content://files/a.pdf"), "a.pdf", "out")
        val saved = store.savePreset("  My preset  ", listOf(task)).getOrThrow()
        assertEquals("My preset", saved.name)
        assertEquals(listOf(task), store.readPresets().single().tasks)
        assertTrue(store.deletePreset(saved.id))
        assertTrue(store.readPresets().isEmpty())
    }

    private suspend fun assertUnreadable(json: String) {
        val store = BatchQueuePresetStore(FakeDao(mutableListOf(row(42, json))))
        val result = loadBatchPresets { store.readPresets() }
        assertTrue("Malformed preset must fail", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("#42"))
    }

    private fun row(id: Long, json: String) =
        BatchPresetEntity(id = id, name = "preset-$id", createdAtMillis = 123L, tasksJson = json)

    private class FakeDao(
        private val rows: MutableList<BatchPresetEntity> = mutableListOf()
    ) : BatchPresetDao {
        var readFailure: Exception? = null
        override suspend fun getAll(): List<BatchPresetEntity> {
            readFailure?.let { throw it }
            return rows.toList()
        }
        override suspend fun insert(preset: BatchPresetEntity): Long {
            val id = (rows.maxOfOrNull { it.id } ?: 0L) + 1L
            rows.add(preset.copy(id = id))
            return id
        }
        override suspend fun deleteById(id: Long): Int =
            if (rows.removeAll { it.id == id }) 1 else 0
    }
}
