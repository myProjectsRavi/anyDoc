package com.docforge.app.batch

import com.docforge.core.storage.db.BatchPresetDao
import com.docforge.core.storage.db.BatchPresetEntity
import org.json.JSONArray
import org.json.JSONObject

class BatchQueuePresetStore(
    private val dao: BatchPresetDao
) {

    suspend fun readPresets(): List<BatchQueuePreset> {
        return runCatching {
            dao.getAll().mapNotNull { entity ->
                val tasks = decodeTasks(entity.tasksJson)
                if (tasks.isEmpty()) null
                else BatchQueuePreset(
                    id = entity.id,
                    name = entity.name,
                    createdAtMillis = entity.createdAtMillis,
                    tasks = tasks
                )
            }
        }.getOrDefault(emptyList())
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
        return runCatching {
            val tasksArray = JSONArray(json)
            buildList {
                for (i in 0 until tasksArray.length()) {
                    val taskObj = tasksArray.optJSONObject(i) ?: continue
                    val typeName = taskObj.optString("type", "")
                    val type = BatchTaskType.entries.firstOrNull { it.name == typeName } ?: continue
                    val inputUrisArray = taskObj.optJSONArray("inputUris") ?: JSONArray()
                    val inputUris = buildList {
                        for (j in 0 until inputUrisArray.length()) {
                            val rawUri = inputUrisArray.optString(j, "").trim()
                            if (rawUri.isNotBlank()) add(rawUri)
                        }
                    }
                    add(
                        BatchQueuePresetTask(
                            type = type,
                            inputUris = inputUris,
                            inputSummary = taskObj.optString("inputSummary", ""),
                            outputBaseName = taskObj.optString("outputBaseName", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
