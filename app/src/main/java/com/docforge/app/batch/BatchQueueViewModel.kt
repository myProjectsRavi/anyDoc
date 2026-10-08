package com.docforge.app.batch

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BatchQueueViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val database = com.docforge.core.storage.db.DocForgeDatabase.get(application.applicationContext)
    private val presetStore = BatchQueuePresetStore(database.batchPresetDao())
    private val persistenceStore = BatchQueuePersistenceStore(database.batchQueueTaskDao())

    private val _uiState = MutableStateFlow(BatchQueueUiState())
    val uiState: StateFlow<BatchQueueUiState> = _uiState.asStateFlow()
    private val _presets = MutableStateFlow<List<BatchQueuePreset>>(emptyList())
    val presets: StateFlow<List<BatchQueuePreset>> = _presets.asStateFlow()

    init {
        viewModelScope.launch {
            refreshPresets()
        }
        viewModelScope.launch {
            collectBatchQueueAfterRecovery(
                recover = { persistenceStore.readRecoverableTasks() },
                restore = { recovered ->
                    BatchQueueRuntimeStore.restoreRecoverableTasksIfEmpty(recovered)
                },
                states = BatchQueueRuntimeStore.state,
                onRecoveryFailure = { error ->
                    setError(error.message ?: "Unable to restore the saved batch queue.")
                },
                onState = { runtimeState ->
                    _uiState.update { current ->
                        runtimeState.copy(errorMessage = current.errorMessage ?: runtimeState.errorMessage)
                    }
                    try {
                        persistenceStore.replaceSnapshot(runtimeState.tasks)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        _uiState.update {
                            it.copy(errorMessage = error.message ?: "Unable to save the batch queue.")
                        }
                    }
                }
            )
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
        _uiState.update { it.copy(errorMessage = null) }
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

        viewModelScope.launch {
            try {
                presetStore.savePreset(name, presetTasks)
                    .onSuccess { preset ->
                        if (refreshPresets()) {
                            BatchQueueRuntimeStore.setStatusMessage("Saved preset: ${preset.name}")
                        }
                    }
                    .onFailure { err ->
                        setError(err.message ?: "Failed to save preset")
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                setError(error.message ?: "Failed to save preset")
            }
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
        viewModelScope.launch {
            try {
                val deleted = presetStore.deletePreset(presetId)
                if (!deleted) {
                    setError("Preset not found.")
                    return@launch
                }
                if (refreshPresets()) {
                    BatchQueueRuntimeStore.setStatusMessage("Deleted preset #$presetId")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                setError(error.message ?: "Failed to delete preset")
            }
        }
    }

    fun runQueue() {
        viewModelScope.launch {
            val startResult = BatchQueueRuntimeStore.beginProcessing()
            val taskIds = startResult.getOrElse { err ->
                setError(err.message ?: "Failed to start queue")
                return@launch
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
    }

    fun cancelQueue() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, BatchQueueForegroundService::class.java).apply {
            action = BatchQueueServiceContract.ACTION_CANCEL_QUEUE
        }
        launchBatchQueueCancellation(
            launch = {
                context.startService(intent)
                    ?: throw IllegalStateException("Batch queue service is unavailable.")
            },
            onFailure = { error ->
                setError("Unable to cancel the batch queue: ${error.message ?: "service unavailable"}")
            }
        )
    }

    fun onNotificationPermissionDenied() {
        setError("Notification permission is required to show batch progress on Android 13+.")
    }

    fun onInputAccessRetentionFailed(error: Throwable) {
        setError(error.message ?: "Unable to retain access to the selected file.")
    }

    private suspend fun refreshPresets(): Boolean =
        loadBatchPresets { presetStore.readPresets() }.fold(
            onSuccess = { loaded ->
                _presets.value = loaded
                true
            },
            onFailure = { error ->
                setError("Unable to load saved presets: ${error.message ?: "storage error"}")
                false
            }
        )

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}
