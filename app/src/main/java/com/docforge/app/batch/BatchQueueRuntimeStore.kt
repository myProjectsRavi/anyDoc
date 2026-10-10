package com.docforge.app.batch

import android.net.Uri
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BatchQueueRuntimeStore {
    private val taskIdCounter = AtomicLong(1L)
    private val outputBaseSanitizer = Regex("[^a-zA-Z0-9_-]")
    private val mutex = Mutex()

    private val _state = MutableStateFlow(BatchQueueUiState())
    val state: StateFlow<BatchQueueUiState> = _state.asStateFlow()

    fun addTask(type: BatchTaskType, inputUris: List<Uri>, inputLabels: List<String>): Result<Unit> {
        val uniqueUris = inputUris.distinct()
        validateInputUris(uniqueUris)?.let { error ->
            return Result.failure(IllegalArgumentException(error))
        }
        val validationError = type.validateInputCount(uniqueUris.size)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val taskId = taskIdCounter.getAndIncrement()
        val summary = buildInputSummary(type, uniqueUris.size, inputLabels)
        val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val outputBaseName = "${type.defaultOutputPrefix}_${timestamp}_$taskId"

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
        while (true) {
            val current = _state.value
            val task = current.tasks.firstOrNull { it.id == taskId }
                ?: return Result.failure(IllegalArgumentException("Task not found."))
            if (task.status == BatchTaskStatus.RUNNING) {
                return Result.failure(IllegalStateException("Cannot remove a running task."))
            }
            val updated = current.copy(
                tasks = current.tasks.filterNot { it.id == taskId },
                statusMessage = "Removed task #$taskId",
                errorMessage = null
            )
            if (_state.compareAndSet(current, updated)) return Result.success(Unit)
        }
    }

    fun moveTaskUp(taskId: Long): Result<Unit> = moveTask(taskId, -1)

    fun moveTaskDown(taskId: Long): Result<Unit> = moveTask(taskId, +1)

    fun updateOutputBaseName(taskId: Long, outputBaseName: String): Result<Unit> {
        while (true) {
            val current = _state.value
            val index = current.tasks.indexOfFirst { it.id == taskId }
            if (index < 0) {
                return Result.failure(IllegalArgumentException("Task not found."))
            }
            val task = current.tasks[index]
            if (task.status != BatchTaskStatus.QUEUED) {
                return Result.failure(IllegalStateException("Only queued tasks can be edited."))
            }
            val sanitized = outputBaseName.trim()
                .replace(outputBaseSanitizer, "_")
                .ifBlank { "${task.type.defaultOutputPrefix}_${task.id}" }
            val tasks = current.tasks.toMutableList()
            tasks[index] = task.copy(outputBaseName = sanitized)
            val updated = current.copy(
                tasks = tasks,
                statusMessage = "Updated output base for task #$taskId",
                errorMessage = null
            )
            if (_state.compareAndSet(current, updated)) return Result.success(Unit)
        }
    }

    fun clearQueue(): Result<Unit> {
        while (true) {
            val current = _state.value
            if (current.isProcessing) {
                return Result.failure(IllegalStateException("Stop processing before clearing the queue."))
            }
            val cleared = BatchQueueUiState(statusMessage = "Queue cleared")
            if (_state.compareAndSet(current, cleared)) return Result.success(Unit)
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun queuedTasksSnapshot(): List<BatchQueueTask> {
        return _state.value.tasks.filter { it.status == BatchTaskStatus.QUEUED }
    }

    fun recoverableTasksSnapshot(): List<BatchQueueTask> {
        return _state.value.tasks.filter {
            it.status == BatchTaskStatus.QUEUED || it.status == BatchTaskStatus.RUNNING
        }
    }

    fun restoreRecoverableTasksIfEmpty(tasks: List<BatchQueueTask>): Boolean {
        if (_state.value.isProcessing || _state.value.tasks.isNotEmpty() || tasks.isEmpty()) {
            return false
        }

        val restored = tasks
            .filter { it.status == BatchTaskStatus.QUEUED }
            .distinctBy { it.id }

        if (restored.isEmpty()) return false

        val restoredMaxId = restored.maxOf { it.id }
        taskIdCounter.updateAndGet { current ->
            nextTaskIdAfterRestore(current, restoredMaxId)
        }

        _state.value = BatchQueueUiState(
            tasks = restored,
            statusMessage = "Recovered ${restored.size} queued task(s) after app restart."
        )
        return true
    }

    fun replaceQueueWithPreset(presetName: String, presetTasks: List<BatchQueuePresetTask>): Result<Unit> {
        if (_state.value.isProcessing) {
            return Result.failure(IllegalStateException("Stop processing before loading a preset."))
        }
        if (presetTasks.isEmpty()) {
            return Result.failure(IllegalArgumentException("Preset has no tasks."))
        }

        // Validate the entire preset before allocating IDs or changing the existing queue.
        val validatedTasks = presetTasks.mapIndexed { index, presetTask ->
            val uris = presetTask.inputUris.map { raw -> Uri.parse(raw.trim()) }
            validateInputUris(uris)?.let { error ->
                return Result.failure(
                    IllegalArgumentException("Preset task ${index + 1} (${presetTask.type.title}): $error")
                )
            }
            // Match recovery's first-seen URI deduplication before checking input counts.
            val uniqueUris = uris.distinct()
            presetTask.type.validateInputCount(uniqueUris.size)?.let { error ->
                return Result.failure(
                    IllegalArgumentException("Preset task ${index + 1} (${presetTask.type.title}): $error")
                )
            }
            presetTask to uniqueUris
        }

        val rebuiltTasks = validatedTasks.map { (presetTask, uris) ->
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

    suspend fun beginProcessing(): Result<List<Long>> = mutex.withLock {
        beginProcessingFromCurrentState()
    }

    private fun beginProcessingFromCurrentState(): Result<List<Long>> {
        while (true) {
            val current = _state.value
            if (current.isProcessing) {
                return Result.failure(IllegalStateException("Queue is already running."))
            }
            val queued = current.tasks
                .filter { it.status == BatchTaskStatus.QUEUED }
                .map { it.id }
            if (queued.isEmpty()) {
                return Result.failure(IllegalArgumentException("Add at least one queued task first."))
            }
            val started = current.copy(
                isProcessing = true,
                processedCount = 0,
                successCount = 0,
                failureCount = 0,
                statusMessage = "Starting batch queue...",
                errorMessage = null
            )
            if (_state.compareAndSet(current, started)) return Result.success(queued)
        }
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
        transitionTaskToTerminal(
            taskId = taskId,
            status = BatchTaskStatus.SUCCESS,
            outputPath = outputPath,
            outputSizeBytes = outputSizeBytes
        )
    }

    fun markTaskFailure(taskId: Long, errorMessage: String) {
        transitionTaskToTerminal(
            taskId = taskId,
            status = BatchTaskStatus.FAILED,
            errorMessage = errorMessage
        )
    }

    fun markTaskCanceled(taskId: Long) {
        transitionTaskToTerminal(
            taskId = taskId,
            status = BatchTaskStatus.CANCELED,
            errorMessage = "Task canceled"
        )
    }

    private fun transitionTaskToTerminal(
        taskId: Long,
        status: BatchTaskStatus,
        outputPath: String? = null,
        outputSizeBytes: Long? = null,
        errorMessage: String? = null
    ) {
        _state.update { state ->
            val index = state.tasks.indexOfFirst {
                it.id == taskId && it.status == BatchTaskStatus.RUNNING
            }
            if (index < 0) return@update state

            val updatedTasks = state.tasks.toMutableList()
            updatedTasks[index] = updatedTasks[index].copy(
                status = status,
                outputPath = outputPath,
                outputSizeBytes = outputSizeBytes,
                errorMessage = errorMessage
            )
            state.copy(
                tasks = updatedTasks,
                processedCount = state.processedCount + 1,
                successCount = state.successCount + if (status == BatchTaskStatus.SUCCESS) 1 else 0,
                failureCount = state.failureCount + if (status == BatchTaskStatus.SUCCESS) 0 else 1
            )
        }
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
        // Validate and reorder against the same snapshot. A failed CAS means another
        // writer changed the queue, so retry with its latest tasks and statuses.
        while (true) {
            val current = _state.value
            val task = current.tasks.firstOrNull { it.id == taskId }
                ?: return Result.failure(IllegalArgumentException("Task not found."))

            if (task.status != BatchTaskStatus.QUEUED) {
                return Result.failure(IllegalStateException("Only queued tasks can be reordered."))
            }

            val queuedIndices = current.tasks.indices.filter { index ->
                current.tasks[index].status == BatchTaskStatus.QUEUED
            }
            val queuedPosition = queuedIndices.indexOfFirst { index ->
                current.tasks[index].id == taskId
            }
            if (queuedPosition < 0) {
                return Result.failure(IllegalStateException("Task is not queued."))
            }

            val targetQueuedPosition = queuedPosition + delta
            if (targetQueuedPosition !in queuedIndices.indices) {
                return Result.failure(IllegalStateException("Task is already at the edge of the queued list."))
            }

            val updated = current.tasks.toMutableList()
            val sourceIndex = queuedIndices[queuedPosition]
            val targetIndex = queuedIndices[targetQueuedPosition]
            val swapped = updated[sourceIndex]
            updated[sourceIndex] = updated[targetIndex]
            updated[targetIndex] = swapped

            if (_state.compareAndSet(
                    current,
                    current.copy(
                        tasks = updated,
                        statusMessage = "Reordered queued tasks.",
                        errorMessage = null
                    )
                )
            ) {
                return Result.success(Unit)
            }
        }
    }

    private fun validateInputUris(uris: List<Uri>): String? =
        if (uris.any { it.toString().isBlank() || it.scheme != "content" }) {
            "Only nonblank content:// input URIs are supported."
        } else {
            null
        }

    private fun buildInputSummary(type: BatchTaskType, count: Int, labels: List<String>): String {
        if (labels.isEmpty()) {
            return "${type.title} ($count input(s))"
        }
        val first = labels.first()
        return if (labels.size == 1) first else "$first +${labels.size - 1} more"
    }
}


internal fun nextTaskIdAfterRestore(currentNextId: Long, restoredMaxId: Long): Long {
    if (restoredMaxId <= 0L) return currentNextId.coerceAtLeast(1L)
    if (restoredMaxId == Long.MAX_VALUE) return Long.MAX_VALUE
    return maxOf(currentNextId, restoredMaxId + 1L)
}
