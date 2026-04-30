package com.docforge.app.batch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BatchQueuePresetStore(
    context: Context
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readPresets(): List<BatchQueuePreset> {
        val raw = prefs.getString(KEY_PRESETS_JSON, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optLong("id", -1L)
                    if (id <= 0L) continue
                    val name = item.optString("name", "").trim().ifBlank { "Preset $id" }
                    val createdAtMillis = item.optLong("createdAtMillis", 0L)
                    val tasksArray = item.optJSONArray("tasks") ?: JSONArray()
                    val tasks = buildList {
                        for (taskIndex in 0 until tasksArray.length()) {
                            val taskObj = tasksArray.optJSONObject(taskIndex) ?: continue
                            val typeName = taskObj.optString("type", "")
                            val type = BatchTaskType.entries.firstOrNull { it.name == typeName } ?: continue
                            val inputUrisArray = taskObj.optJSONArray("inputUris") ?: JSONArray()
                            val inputUris = buildList {
                                for (uriIndex in 0 until inputUrisArray.length()) {
                                    val rawUri = inputUrisArray.optString(uriIndex, "").trim()
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
                    if (tasks.isNotEmpty()) {
                        add(
                            BatchQueuePreset(
                                id = id,
                                name = name,
                                createdAtMillis = createdAtMillis,
                                tasks = tasks
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePreset(name: String, tasks: List<BatchQueuePresetTask>): Result<BatchQueuePreset> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Preset name cannot be empty."))
        }
        if (tasks.isEmpty()) {
            return Result.failure(IllegalArgumentException("Queue has no tasks to save."))
        }

        val current = readPresets().toMutableList()
        val newId = prefs.getLong(KEY_NEXT_ID, 1L).coerceAtLeast(1L)
        val preset = BatchQueuePreset(
            id = newId,
            name = trimmedName,
            createdAtMillis = System.currentTimeMillis(),
            tasks = tasks
        )
        current += preset
        writePresets(current)
        prefs.edit().putLong(KEY_NEXT_ID, newId + 1L).apply()
        return Result.success(preset)
    }

    fun deletePreset(id: Long): Boolean {
        val current = readPresets()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) {
            return false
        }
        writePresets(updated)
        return true
    }

    private fun writePresets(presets: List<BatchQueuePreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            val taskArray = JSONArray()
            preset.tasks.forEach { task ->
                val taskObj = JSONObject()
                    .put("type", task.type.name)
                    .put("inputSummary", task.inputSummary)
                    .put("outputBaseName", task.outputBaseName)
                val urisArray = JSONArray()
                task.inputUris.forEach { uri -> urisArray.put(uri) }
                taskObj.put("inputUris", urisArray)
                taskArray.put(taskObj)
            }

            val presetObj = JSONObject()
                .put("id", preset.id)
                .put("name", preset.name)
                .put("createdAtMillis", preset.createdAtMillis)
                .put("tasks", taskArray)
            array.put(presetObj)
        }
        prefs.edit().putString(KEY_PRESETS_JSON, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "docforge_batch_presets"
        const val KEY_PRESETS_JSON = "batch_presets_json"
        const val KEY_NEXT_ID = "batch_presets_next_id"
    }
}
