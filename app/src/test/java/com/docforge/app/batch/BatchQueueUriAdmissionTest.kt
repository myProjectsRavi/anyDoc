package com.docforge.app.batch

import android.net.Uri
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
class BatchQueueUriAdmissionTest {
    @Before
    fun setUp() {
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    @After
    fun tearDown() {
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    @Test
    fun directEnqueueRejectsBlankFileHttpAndRelativeUrisWithoutChangingQueue() {
        val invalid = listOf("", "  ", "file:///tmp/private.pdf", "https://example.com/a.pdf", "relative.pdf")
        invalid.forEach { raw ->
            val before = BatchQueueRuntimeStore.state.value
            val result = BatchQueueRuntimeStore.addTask(
                BatchTaskType.PDF_COMPRESS, listOf(Uri.parse(raw)), listOf("invalid.pdf")
            )
            assertTrue("Expected rejection for '$raw'", result.isFailure)
            assertEquals(before, BatchQueueRuntimeStore.state.value)
        }
    }

    @Test
    fun mixedDirectInputFailsAtomicallyWithoutConsumingAnId() {
        val first = Uri.parse("content://provider/first.pdf")
        assertTrue(BatchQueueRuntimeStore.addTask(BatchTaskType.PDF_COMPRESS, listOf(first), emptyList()).isSuccess)
        val previous = BatchQueueRuntimeStore.state.value
        val lastId = previous.tasks.single().id

        val result = BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_MERGE,
            listOf(first, Uri.parse("file:///tmp/second.pdf")),
            emptyList()
        )
        assertTrue(result.isFailure)
        assertEquals(previous, BatchQueueRuntimeStore.state.value)

        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS, listOf(Uri.parse("content://provider/next.pdf")), emptyList()
        ).isSuccess)
        assertEquals(lastId + 1, BatchQueueRuntimeStore.state.value.tasks.last().id)
    }

    @Test
    fun validDirectContentUrisRemainDeduplicatedAndRecoverable() {
        val first = Uri.parse("content://provider/first.pdf")
        val second = Uri.parse("content://provider/second.pdf")
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_MERGE, listOf(first, first, second), listOf("first.pdf", "second.pdf")
        ).isSuccess)
        val task = BatchQueueRuntimeStore.state.value.tasks.single()
        assertEquals(listOf(first, second), task.inputUris)
        assertEquals(listOf(task.id), BatchQueuePersistenceMapper.fromEntities(
            BatchQueuePersistenceMapper.toEntities(listOf(task))
        ).map { it.id })
    }

    @Test
    fun invalidPresetUriInLaterTaskPreservesExistingQueueAndNextId() {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS, listOf(Uri.parse("content://provider/original.pdf")), emptyList()
        ).isSuccess)
        val previous = BatchQueueRuntimeStore.state.value
        val lastId = previous.tasks.single().id
        val result = BatchQueueRuntimeStore.replaceQueueWithPreset(
            "broken",
            listOf(
                preset("content://provider/valid.pdf"),
                preset("https://example.com/not-content.pdf")
            )
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Preset task 2"))
        assertEquals(previous, BatchQueueRuntimeStore.state.value)

        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS, listOf(Uri.parse("content://provider/next.pdf")), emptyList()
        ).isSuccess)
        assertEquals(lastId + 1, BatchQueueRuntimeStore.state.value.tasks.last().id)
    }

    @Test
    fun blankPresetEntryCannotBeSilentlyDroppedEvenIfInputCountWouldPass() {
        val previous = BatchQueueRuntimeStore.state.value
        val result = BatchQueueRuntimeStore.replaceQueueWithPreset(
            "broken",
            listOf(BatchQueuePresetTask(
                type = BatchTaskType.PDF_MERGE,
                inputUris = listOf("content://provider/first.pdf", " ", "content://provider/second.pdf"),
                inputSummary = "three entries",
                outputBaseName = "merge"
            ))
        )
        assertTrue(result.isFailure)
        assertEquals(previous, BatchQueueRuntimeStore.state.value)
    }

    @Test
    fun validPresetReplayPreservesOrderNamesAndRecoveryCompatibility() {
        val result = BatchQueueRuntimeStore.replaceQueueWithPreset(
            "valid",
            listOf(
                preset("  content://provider/first.pdf  ", "first_output"),
                preset("content://provider/second.pdf", "second_output")
            )
        )
        assertTrue(result.isSuccess)
        val tasks = BatchQueueRuntimeStore.state.value.tasks
        assertEquals(listOf("first_output", "second_output"), tasks.map { it.outputBaseName })
        assertEquals(listOf(
            "content://provider/first.pdf", "content://provider/second.pdf"
        ), tasks.map { it.inputUris.single().toString() })
        assertFalse(BatchQueueRuntimeStore.state.value.isProcessing)
        assertEquals(tasks.map { it.id }, BatchQueuePersistenceMapper.fromEntities(
            BatchQueuePersistenceMapper.toEntities(tasks)
        ).map { it.id })
    }

    @Test
    fun duplicateMergePresetCannotReplaceQueueOrConsumeTaskIds() {
        val original = Uri.parse("content://provider/original.pdf")
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS, listOf(original), emptyList()
        ).isSuccess)
        val previous = BatchQueueRuntimeStore.state.value
        val previousId = previous.tasks.single().id

        val result = BatchQueueRuntimeStore.replaceQueueWithPreset(
            "duplicate merge",
            listOf(BatchQueuePresetTask(
                type = BatchTaskType.PDF_MERGE,
                inputUris = listOf("content://provider/a.pdf", " content://provider/a.pdf "),
                inputSummary = "two entries, one document",
                outputBaseName = "merge"
            ))
        )
        assertTrue(result.isFailure)
        assertEquals(previous, BatchQueueRuntimeStore.state.value)
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/next.pdf")), emptyList()
        ).isSuccess)
        assertEquals(previousId + 1, BatchQueueRuntimeStore.state.value.tasks.last().id)
    }

    @Test
    fun validMergePresetDeduplicatesUrisBeforeRecoveryInFirstSeenOrder() {
        val first = Uri.parse("content://provider/first.pdf")
        val second = Uri.parse("content://provider/second.pdf")
        val result = BatchQueueRuntimeStore.replaceQueueWithPreset(
            "deduplicated merge",
            listOf(BatchQueuePresetTask(
                type = BatchTaskType.PDF_MERGE,
                inputUris = listOf(first.toString(), "  ${first}  ", second.toString()),
                inputSummary = "merge",
                outputBaseName = "merged"
            ))
        )
        assertTrue(result.isSuccess)
        val task = BatchQueueRuntimeStore.state.value.tasks.single()
        assertEquals(listOf(first, second), task.inputUris)
        val recovered = BatchQueuePersistenceMapper.fromEntities(
            BatchQueuePersistenceMapper.toEntities(listOf(task))
        )
        assertEquals(listOf(first, second), recovered.single().inputUris)
    }

    @Test
    fun singleInputPresetWithDuplicateUriUsesOneEffectiveInput() {
        val uri = Uri.parse("content://provider/one.pdf")
        val result = BatchQueueRuntimeStore.replaceQueueWithPreset(
            "duplicate single",
            listOf(BatchQueuePresetTask(
                type = BatchTaskType.PDF_COMPRESS,
                inputUris = listOf(uri.toString(), uri.toString()),
                inputSummary = "one file",
                outputBaseName = "compressed"
            ))
        )
        assertTrue(result.isSuccess)
        val task = BatchQueueRuntimeStore.state.value.tasks.single()
        assertEquals(listOf(uri), task.inputUris)
        assertEquals(listOf(uri), BatchQueuePersistenceMapper.fromEntities(
            BatchQueuePersistenceMapper.toEntities(listOf(task))
        ).single().inputUris)
    }

    private fun preset(uri: String, output: String = "compressed") = BatchQueuePresetTask(
        type = BatchTaskType.PDF_COMPRESS,
        inputUris = listOf(uri),
        inputSummary = "input.pdf",
        outputBaseName = output
    )
}
