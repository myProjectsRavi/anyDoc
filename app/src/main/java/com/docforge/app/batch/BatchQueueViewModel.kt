package com.docforge.app.batch

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BatchQueueViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val presetStore = BatchQueuePresetStore(application.applicationContext)

    private val _uiState = MutableStateFlow(BatchQueueUiState())
    val uiState: StateFlow<BatchQueueUiState> = _uiState.asStateFlow()
    private val _presets = MutableStateFlow(presetStore.readPresets())
    val presets: StateFlow<List<BatchQueuePreset>> = _presets.asStateFlow()

    init {
        viewModelScope.launch {
            BatchQueueRuntimeStore.state.collect { runtimeState ->
                _uiState.value = runtimeState
            }
        }
    }

    fun addTask(
        type: BatchTaskType,
        inputUris: List<Uri>,
        inputLabels: List<String> = emptyList()
    ) {
        BatchQueueRuntimeStore.addTask(type, inputUris, inputLabels)
            .onFailure { err -> setError(err.message ?: "Failed to add task") }
    }

    fun removeTask(taskId: Long) {
        BatchQueueRuntimeStore.removeTask(taskId)
            .onFailure { err -> setError(err.message ?: "Failed to remove task") }
    }

    fun moveTaskUp(taskId: Long) {
        BatchQueueRuntimeStore.moveTaskUp(taskId)
            .onFailure { err -> setError(err.message ?: "Failed to move task up") }
    }

    fun moveTaskDown(taskId: Long) {
        BatchQueueRuntimeStore.moveTaskDown(taskId)
            .onFailure { err -> setError(err.message ?: "Failed to move task down") }
    }

    fun updateOutputBaseName(taskId: Long, outputBaseName: String) {
        BatchQueueRuntimeStore.updateOutputBaseName(taskId, outputBaseName)
            .onFailure { err -> setError(err.message ?: "Failed to update output name") }
    }

    fun clearQueue() {
        BatchQueueRuntimeStore.clearQueue()
            .onFailure { err -> setError(err.message ?: "Failed to clear queue") }
    }

    fun clearError() {
        BatchQueueRuntimeStore.clearError()
    }

    fun saveQueuedTasksAsPreset(name: String) {
        val queuedTasks = BatchQueueRuntimeStore.queuedTasksSnapshot()
        val presetTasks = queuedTasks.map { task ->
            BatchQueuePresetTask(
                type = task.type,
                inputUris = task.inputUris.map { it.toString() },
                inputSummary = task.inputSummary,
                outputBaseName = task.outputBaseName
            )
        }

        presetStore.savePreset(name, presetTasks)
            .onSuccess { preset ->
                _presets.value = presetStore.readPresets()
                BatchQueueRuntimeStore.setStatusMessage("Saved preset: ${preset.name}")
            }
            .onFailure { err ->
                setError(err.message ?: "Failed to save preset")
            }
    }

    fun loadPreset(presetId: Long) {
        val preset = _presets.value.firstOrNull { it.id == presetId } ?: run {
            setError("Preset not found.")
            return
        }

        BatchQueueRuntimeStore.replaceQueueWithPreset(
            presetName = preset.name,
            presetTasks = preset.tasks
        ).onFailure { err ->
            setError(err.message ?: "Failed to load preset")
        }
    }

    fun deletePreset(presetId: Long) {
        val deleted = presetStore.deletePreset(presetId)
        if (!deleted) {
            setError("Preset not found.")
            return
        }
        _presets.value = presetStore.readPresets()
        BatchQueueRuntimeStore.setStatusMessage("Deleted preset #$presetId")
    }

    fun runQueue() {
        val startResult = BatchQueueRuntimeStore.beginProcessing()
        val taskIds = startResult.getOrElse { err ->
            setError(err.message ?: "Failed to start queue")
            return
        }

        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, BatchQueueForegroundService::class.java).apply {
            action = BatchQueueServiceContract.ACTION_RUN_QUEUE
            putExtra(BatchQueueServiceContract.EXTRA_TASK_IDS, taskIds.toLongArray())
        }

        runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.onFailure { err ->
            BatchQueueRuntimeStore.failProcessing(err.message ?: "Unable to start batch service")
        }
    }

    fun cancelQueue() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, BatchQueueForegroundService::class.java).apply {
            action = BatchQueueServiceContract.ACTION_CANCEL_QUEUE
        }
        context.startService(intent)
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}
