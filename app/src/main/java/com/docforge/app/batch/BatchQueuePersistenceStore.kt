package com.docforge.app.batch

import android.net.Uri
import com.docforge.core.storage.db.BatchQueueTaskDao
import com.docforge.core.storage.db.BatchQueueTaskEntity
import org.json.JSONArray
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class BatchQueuePersistenceStore(
    private val dao: BatchQueueTaskDao
) {
    suspend fun replaceSnapshot(currentTasks: () -> List<BatchQueueTask>) {
        persistLatestSnapshot(writeMutex, currentTasks) { tasks ->
            dao.replaceAll(BatchQueuePersistenceMapper.toEntities(tasks))
        }
    }

    suspend fun readRecoverableTasks(): List<BatchQueueTask> {
        return BatchQueuePersistenceMapper.fromEntities(dao.getAll())
    }

    private companion object {
        val writeMutex = Mutex()
    }
}

/**
 * Evaluate the current snapshot only after obtaining the shared write lock.
 * Capturing it before the lock can let an older writer overwrite newer queue state.
 */
internal suspend fun <T> persistLatestSnapshot(
    mutex: Mutex,
    currentSnapshot: () -> List<T>,
    persist: suspend (List<T>) -> Unit
) {
    mutex.withLock {
        persist(currentSnapshot())
    }
}

internal object BatchQueuePersistenceMapper {
    private val SAFE_OUTPUT_BASE = Regex("[a-zA-Z0-9_-]+")
    private const val RECOVERED_RUNNING_MESSAGE =
        "Recovered after app restart. Review this task before running it again."

    fun toEntities(
        tasks: List<BatchQueueTask>,
        snapshotAtMillis: Long = System.currentTimeMillis()
    ): List<BatchQueueTaskEntity> {
        return tasks
            .mapIndexed { index, task ->
                BatchQueueTaskEntity(
                    id = task.id,
                    taskType = task.type.name,
                    inputUrisJson = JSONArray(task.inputUris.map(Uri::toString)).toString(),
                    inputSummary = task.inputSummary,
                    outputBaseName = task.outputBaseName,
                    status = task.status.name,
                    outputPath = task.outputPath,
                    outputSizeBytes = task.outputSizeBytes ?: 0L,
                    errorMessage = task.errorMessage,
                    createdAtMillis = snapshotAtMillis + index
                )
            }
    }

    fun fromEntities(entities: List<BatchQueueTaskEntity>): List<BatchQueueTask> {
        return entities.mapNotNull(::fromEntity)
    }

    private fun fromEntity(entity: BatchQueueTaskEntity): BatchQueueTask? {
        val persistedStatus = BatchTaskStatus.entries.firstOrNull { it.name == entity.status }
            ?: throw IllegalStateException("Saved batch task #${entity.id} has an unknown status.")
        // Completed tasks are deliberately not requeued after process death.
        if (persistedStatus != BatchTaskStatus.QUEUED && persistedStatus != BatchTaskStatus.RUNNING) {
            return null
        }

        fun invalid(reason: String): Nothing =
            throw IllegalStateException("Saved batch task #${entity.id} is invalid: $reason")

        if (
            entity.id <= 0L ||
            entity.id == Long.MAX_VALUE ||
            entity.outputBaseName.isBlank() ||
            !SAFE_OUTPUT_BASE.matches(entity.outputBaseName)
        ) invalid("task ID or output name")

        val type = BatchTaskType.entries.firstOrNull { it.name == entity.taskType }
            ?: invalid("unknown task type")
        val uris = try {
            val array = JSONArray(entity.inputUrisJson)
            buildList {
                repeat(array.length()) { index ->
                    val raw = array.optString(index).trim()
                    if (raw.isBlank()) invalid("empty input URI")
                    val uri = Uri.parse(raw)
                    if (uri.scheme != "content") invalid("unsupported input URI")
                    add(uri)
                }
            }.distinct()
        } catch (error: Exception) {
            throw IllegalStateException("Saved batch task #${entity.id} has invalid input URIs.", error)
        }

        if (uris.isEmpty() || type.validateInputCount(uris.size) != null) {
            invalid("input count")
        }

        val recoveredFromRunning = persistedStatus == BatchTaskStatus.RUNNING
        return BatchQueueTask(
            id = entity.id,
            type = type,
            inputUris = uris,
            inputSummary = entity.inputSummary.ifBlank { "${type.title} (${uris.size} input(s))" },
            outputBaseName = entity.outputBaseName,
            status = BatchTaskStatus.QUEUED,
            outputPath = null,
            outputSizeBytes = null,
            errorMessage = if (recoveredFromRunning) RECOVERED_RUNNING_MESSAGE else null
        )
    }
}
