package com.docforge.app.batch

import com.docforge.core.storage.db.BatchPresetDao
import com.docforge.core.storage.db.BatchPresetEntity
import org.json.JSONArray
import org.json.JSONObject

class BatchQueuePresetStore(
    private val dao: BatchPresetDao
) {

    suspend fun readPresets(): List<BatchQueuePreset> =
        dao.getAll().map { entity ->
            val tasks = try {
                decodeTasks(entity.tasksJson)
            } catch (error: Exception) {
                throw IllegalStateException(
                    "Saved preset '${entity.name}' (#${entity.id}) contains unreadable tasks.",
                    error
                )
            }
            BatchQueuePreset(
                id = entity.id,
                name = entity.name,
                createdAtMillis = entity.createdAtMillis,
                tasks = tasks
            )
        }

    suspend fun savePreset(name: String, tasks: List<BatchQueuePresetTask>): Result<BatchQueuePreset> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Preset name cannot be empty."))
        }
        if (tasks.isEmpty()) {
            return Result.failure(IllegalArgumentException("Queue has no tasks to save."))
        }

        val entity = BatchPresetEntity(
            name = trimmedName,
            createdAtMillis = System.currentTimeMillis(),
            tasksJson = encodeTasks(tasks)
        )
        val newId = dao.insert(entity)
        val preset = BatchQueuePreset(
            id = newId,
            name = trimmedName,
            createdAtMillis = entity.createdAtMillis,
            tasks = tasks
        )
        return Result.success(preset)
    }

    suspend fun deletePreset(id: Long): Boolean {
        return dao.deleteById(id) > 0
    }

    private fun encodeTasks(tasks: List<BatchQueuePresetTask>): String {
        val array = JSONArray()
        tasks.forEach { task ->
            val taskObj = JSONObject()
                .put("type", task.type.name)
                .put("inputSummary", task.inputSummary)
                .put("outputBaseName", task.outputBaseName)
            val urisArray = JSONArray()
            task.inputUris.forEach { uri -> urisArray.put(uri) }
            taskObj.put("inputUris", urisArray)
            array.put(taskObj)
        }
        return array.toString()
    }

    private fun decodeTasks(json: String): List<BatchQueuePresetTask> {
        val tasksArray = JSONArray(json)
        require(tasksArray.length() > 0) { "Preset has no tasks." }
        return buildList {
            for (i in 0 until tasksArray.length()) {
                val taskObj = tasksArray.getJSONObject(i)
                val rawType = taskObj.get("type")
                require(rawType is String) { "Invalid task type at index $i." }
                val type = BatchTaskType.entries.firstOrNull { it.name == rawType }
                    ?: throw IllegalArgumentException("Unknown task type at index $i.")
                val urisArray = taskObj.getJSONArray("inputUris")
                val inputUris = buildList {
                    for (j in 0 until urisArray.length()) {
                        val rawUri = urisArray.get(j)
                        require(rawUri is String && rawUri.isNotBlank()) {
                            "Invalid input URI at task $i, position $j."
                        }
                        add(rawUri.trim())
                    }
                }
                require(type.validateInputCount(inputUris.size) == null) {
                    "Invalid input count at task $i."
                }
                val inputSummary = taskObj.get("inputSummary")
                val outputBaseName = taskObj.get("outputBaseName")
                require(inputSummary is String && outputBaseName is String) {
                    "Invalid task metadata at index $i."
                }
                add(
                    BatchQueuePresetTask(
                        type = type,
                        inputUris = inputUris,
                        inputSummary = inputSummary,
                        outputBaseName = outputBaseName
                    )
                )
            }
        }
    }
}

/** Convert storage errors to a UI result without swallowing coroutine cancellation. */
internal suspend fun loadBatchPresets(
    read: suspend () -> List<BatchQueuePreset>
): Result<List<BatchQueuePreset>> = try {
    Result.success(read())
} catch (cancelled: kotlinx.coroutines.CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
