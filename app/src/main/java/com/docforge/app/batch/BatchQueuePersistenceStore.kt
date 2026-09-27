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
    suspend fun replaceSnapshot(tasks: List<BatchQueueTask>) {
        writeMutex.withLock {
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

internal object BatchQueuePersistenceMapper {
    private const val RECOVERED_RUNNING_MESSAGE =
        "Recovered after app restart. Review this task before running it again."

    fun toEntities(
        tasks: List<BatchQueueTask>,
        snapshotAtMillis: Long = System.currentTimeMillis()
    ): List<BatchQueueTaskEntity> {
        return tasks
            .filter { task ->
                task.status == BatchTaskStatus.QUEUED || task.status == BatchTaskStatus.RUNNING
            }
            .mapIndexed { index, task ->
                BatchQueueTaskEntity(
                    id = task.id,
                    taskType = task.type.name,
                    inputUrisJson = JSONArray(task.inputUris.map(Uri::toString)).toString(),
                    inputSummary = task.inputSummary,
                    outputBaseName = task.outputBaseName,
                    status = task.status.name,
                    outputPath = null,
                    outputSizeBytes = 0L,
                    errorMessage = null,
                    createdAtMillis = snapshotAtMillis + index
                )
            }
    }

    fun fromEntities(entities: List<BatchQueueTaskEntity>): List<BatchQueueTask> {
        return entities.mapNotNull(::fromEntity)
    }

    private fun fromEntity(entity: BatchQueueTaskEntity): BatchQueueTask? {
        if (entity.id <= 0L || entity.outputBaseName.isBlank()) return null

        val type = BatchTaskType.entries.firstOrNull { it.name == entity.taskType } ?: return null
        val persistedStatus = BatchTaskStatus.entries.firstOrNull { it.name == entity.status } ?: return null
        if (persistedStatus != BatchTaskStatus.QUEUED && persistedStatus != BatchTaskStatus.RUNNING) {
            return null
        }

        val uris = runCatching {
            val array = JSONArray(entity.inputUrisJson)
            buildList {
                repeat(array.length()) { index ->
                    val raw = array.optString(index).trim()
                    if (raw.isBlank()) return@runCatching emptyList<Uri>()
                    val uri = Uri.parse(raw)
                    if (uri.scheme != "content") return@runCatching emptyList<Uri>()
                    add(uri)
                }
            }
        }.getOrNull() ?: return null

        if (uris.isEmpty() || type.validateInputCount(uris.size) != null) return null

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
