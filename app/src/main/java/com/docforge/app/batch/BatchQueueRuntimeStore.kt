package com.docforge.app.batch

import android.net.Uri
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object BatchQueueRuntimeStore {
    private val taskIdCounter = AtomicLong(1L)
    private val outputBaseSanitizer = Regex("[^a-zA-Z0-9_-]")

    private val _state = MutableStateFlow(BatchQueueUiState())
    val state: StateFlow<BatchQueueUiState> = _state.asStateFlow()

    fun addTask(type: BatchTaskType, inputUris: List<Uri>, inputLabels: List<String>): Result<Unit> {
        val uniqueUris = inputUris.distinct()
        val validationError = type.validateInputCount(uniqueUris.size)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val taskId = taskIdCounter.getAndIncrement()
        val summary = buildInputSummary(type, uniqueUris.size, inputLabels)
        val outputBaseName = "${type.defaultOutputPrefix}_${System.currentTimeMillis()}_$taskId"

        val task = BatchQueueTask(
            id = taskId,
            type = type,
            inputUris = uniqueUris,
            inputSummary = summary,
            outputBaseName = outputBaseName,
            status = BatchTaskStatus.QUEUED
        )

        _state.update {
            it.copy(
                tasks = it.tasks + task,
                statusMessage = "Queued: ${type.title}",
                errorMessage = null
            )
        }
        return Result.success(Unit)
    }

    fun removeTask(taskId: Long): Result<Unit> {
        val current = _state.value
        val task = current.tasks.firstOrNull { it.id == taskId }
            ?: return Result.failure(IllegalArgumentException("Task not found."))

        if (current.isProcessing && task.status == BatchTaskStatus.RUNNING) {
            return Result.failure(IllegalStateException("Cannot remove a running task."))
        }

        _state.update {
            it.copy(
                tasks = it.tasks.filterNot { item -> item.id == taskId },
                statusMessage = "Removed task #$taskId",
                errorMessage = null
            )
        }
        return Result.success(Unit)
    }

    fun moveTaskUp(taskId: Long): Result<Unit> = moveTask(taskId, -1)

    fun moveTaskDown(taskId: Long): Result<Unit> = moveTask(taskId, +1)

    fun updateOutputBaseName(taskId: Long, outputBaseName: String): Result<Unit> {
        val current = _state.value
        val task = current.tasks.firstOrNull { it.id == taskId }
            ?: return Result.failure(IllegalArgumentException("Task not found."))

        if (task.status != BatchTaskStatus.QUEUED) {
            return Result.failure(IllegalStateException("Only queued tasks can be edited."))
        }

        val sanitized = outputBaseName.trim()
            .replace(outputBaseSanitizer, "_")
            .ifBlank { "${task.type.defaultOutputPrefix}_${task.id}" }

        updateTask(taskId) { it.copy(outputBaseName = sanitized) }
        _state.update {
            it.copy(
                statusMessage = "Updated output base for task #$taskId",
                errorMessage = null
            )
        }
        return Result.success(Unit)
    }

    fun clearQueue(): Result<Unit> {
        if (_state.value.isProcessing) {
            return Result.failure(IllegalStateException("Stop processing before clearing the queue."))
        }
        _state.value = BatchQueueUiState(statusMessage = "Queue cleared")
        return Result.success(Unit)
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun queuedTasksSnapshot(): List<BatchQueueTask> {
        return _state.value.tasks.filter { it.status == BatchTaskStatus.QUEUED }
    }

    fun replaceQueueWithPreset(presetName: String, presetTasks: List<BatchQueuePresetTask>): Result<Unit> {
        if (_state.value.isProcessing) {
            return Result.failure(IllegalStateException("Stop processing before loading a preset."))
        }
        if (presetTasks.isEmpty()) {
            return Result.failure(IllegalArgumentException("Preset has no tasks."))
        }

        val rebuiltTasks = presetTasks.map { presetTask ->
            val uris = presetTask.inputUris
                .mapNotNull { raw ->
                    val trimmed = raw.trim()
                    if (trimmed.isBlank()) null else Uri.parse(trimmed)
                }

            val validationError = presetTask.type.validateInputCount(uris.size)
            if (validationError != null) {
                return Result.failure(IllegalArgumentException("Preset task ${presetTask.type.title}: $validationError"))
            }

            val taskId = taskIdCounter.getAndIncrement()
            BatchQueueTask(
                id = taskId,
                type = presetTask.type,
                inputUris = uris,
                inputSummary = presetTask.inputSummary.ifBlank {
                    buildInputSummary(presetTask.type, uris.size, emptyList())
                },
                outputBaseName = presetTask.outputBaseName.trim()
                    .replace(outputBaseSanitizer, "_")
                    .ifBlank { "${presetTask.type.defaultOutputPrefix}_$taskId" },
                status = BatchTaskStatus.QUEUED
            )
        }

        _state.value = BatchQueueUiState(
            tasks = rebuiltTasks,
            statusMessage = "Loaded preset: $presetName (${rebuiltTasks.size} task(s))"
        )
        return Result.success(Unit)
    }

    fun beginProcessing(): Result<List<Long>> {
        if (_state.value.isProcessing) {
            return Result.failure(IllegalStateException("Queue is already running."))
        }

        val queued = _state.value.tasks
            .filter { it.status == BatchTaskStatus.QUEUED }
            .map { it.id }

        if (queued.isEmpty()) {
            return Result.failure(IllegalArgumentException("Add at least one queued task first."))
        }

        _state.update {
            it.copy(
                isProcessing = true,
                processedCount = 0,
                successCount = 0,
                failureCount = 0,
                statusMessage = "Starting batch queue...",
                errorMessage = null
            )
        }

        return Result.success(queued)
    }

    fun snapshotTasks(taskIds: List<Long>): List<BatchQueueTask> {
        val set = taskIds.toSet()
        return _state.value.tasks.filter { task -> task.id in set }
    }

    fun startNextQueuedTask(taskIds: Set<Long>, total: Int): BatchQueueTask? {
        var nextTask: BatchQueueTask? = null
        _state.update { state ->
            val index = state.tasks.indexOfFirst { task ->
                task.id in taskIds && task.status == BatchTaskStatus.QUEUED
            }
            if (index < 0) {
                return@update state
            }

            val running = state.tasks[index].copy(
                status = BatchTaskStatus.RUNNING,
                errorMessage = null
            )
            nextTask = running

            val updatedTasks = state.tasks.toMutableList()
            updatedTasks[index] = running

            state.copy(
                tasks = updatedTasks,
                statusMessage = "Running ${state.processedCount + 1}/$total: ${running.type.title}",
                errorMessage = null
            )
        }
        return nextTask
    }

    fun markTaskRunning(taskId: Long, index: Int, total: Int) {
        updateTask(taskId) { it.copy(status = BatchTaskStatus.RUNNING, errorMessage = null) }
        _state.update {
            it.copy(statusMessage = "Running $index/$total: ${findTaskTitle(taskId)}")
        }
    }

    fun markTaskSuccess(taskId: Long, outputPath: String, outputSizeBytes: Long) {
        updateTask(taskId) {
            it.copy(
                status = BatchTaskStatus.SUCCESS,
                outputPath = outputPath,
                outputSizeBytes = outputSizeBytes,
                errorMessage = null
            )
        }
        incrementCounters(successDelta = 1, failureDelta = 0)
    }

    fun markTaskFailure(taskId: Long, errorMessage: String) {
        updateTask(taskId) {
            it.copy(
                status = BatchTaskStatus.FAILED,
                errorMessage = errorMessage
            )
        }
        incrementCounters(successDelta = 0, failureDelta = 1)
    }

    fun markTaskCanceled(taskId: Long) {
        updateTask(taskId) {
            it.copy(
                status = BatchTaskStatus.CANCELED,
                errorMessage = "Task canceled"
            )
        }
        incrementCounters(successDelta = 0, failureDelta = 1)
    }

    fun markRemainingQueuedAsCanceled(exceptTaskId: Long? = null) {
        _state.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.status == BatchTaskStatus.QUEUED && task.id != exceptTaskId) {
                        task.copy(status = BatchTaskStatus.CANCELED, errorMessage = "Queue canceled")
                    } else {
                        task
                    }
                }
            )
        }
    }

    fun finishProcessing(statusMessage: String) {
        _state.update {
            it.copy(
                isProcessing = false,
                statusMessage = statusMessage,
                errorMessage = null
            )
        }
    }

    fun failProcessing(message: String) {
        _state.update {
            it.copy(
                isProcessing = false,
                statusMessage = null,
                errorMessage = message
            )
        }
    }

    fun setStatusMessage(message: String) {
        _state.update { it.copy(statusMessage = message, errorMessage = null) }
    }

    private fun incrementCounters(successDelta: Int, failureDelta: Int) {
        _state.update {
            it.copy(
                processedCount = it.processedCount + 1,
                successCount = it.successCount + successDelta,
                failureCount = it.failureCount + failureDelta
            )
        }
    }

    private fun updateTask(taskId: Long, transform: (BatchQueueTask) -> BatchQueueTask) {
        _state.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == taskId) transform(task) else task
                }
            )
        }
    }

    private fun findTaskTitle(taskId: Long): String {
        return _state.value.tasks.firstOrNull { it.id == taskId }?.type?.title ?: "Task"
    }

    private fun moveTask(taskId: Long, delta: Int): Result<Unit> {
        val state = _state.value
        val task = state.tasks.firstOrNull { it.id == taskId }
            ?: return Result.failure(IllegalArgumentException("Task not found."))

        if (task.status != BatchTaskStatus.QUEUED) {
            return Result.failure(IllegalStateException("Only queued tasks can be reordered."))
        }

        val queuedIndices = state.tasks
            .mapIndexedNotNull { index, queuedTask ->
                if (queuedTask.status == BatchTaskStatus.QUEUED) index else null
            }
        val queuedPosition = queuedIndices.indexOfFirst { state.tasks[it].id == taskId }
        if (queuedPosition < 0) {
            return Result.failure(IllegalStateException("Task is not queued."))
        }

        val targetQueuedPosition = queuedPosition + delta
        if (targetQueuedPosition !in queuedIndices.indices) {
            return Result.failure(IllegalStateException("Task is already at the edge of the queued list."))
        }

        val sourceIndex = queuedIndices[queuedPosition]
        val targetIndex = queuedIndices[targetQueuedPosition]
        val updated = state.tasks.toMutableList()
        val temp = updated[sourceIndex]
        updated[sourceIndex] = updated[targetIndex]
        updated[targetIndex] = temp

        _state.update {
            it.copy(
                tasks = updated,
                statusMessage = "Reordered queued tasks.",
                errorMessage = null
            )
        }
        return Result.success(Unit)
    }

    private fun buildInputSummary(type: BatchTaskType, count: Int, labels: List<String>): String {
        if (labels.isEmpty()) {
            return "${type.title} ($count input(s))"
        }
        val first = labels.first()
        return if (labels.size == 1) first else "$first +${labels.size - 1} more"
    }
}
