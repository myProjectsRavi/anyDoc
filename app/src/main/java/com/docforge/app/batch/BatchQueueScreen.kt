package com.docforge.app.batch

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BatchQueueRoute(
    viewModel: BatchQueueViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val presets = viewModel.presets.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedTaskType by remember { mutableStateOf(BatchTaskType.IMAGES_TO_PDF) }
    var pendingTaskType by remember { mutableStateOf<BatchTaskType?>(null) }
    var presetName by remember { mutableStateOf(defaultPresetName()) }

    val singlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val type = pendingTaskType ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.addTask(
                type = type,
                inputUris = listOf(uri),
                inputLabels = listOf(readSourceLabel(context, uri))
            )
        }
        pendingTaskType = null
    }

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val type = pendingTaskType ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) {
            viewModel.addTask(
                type = type,
                inputUris = uris,
                inputLabels = uris.map { uri -> readSourceLabel(context, uri) }
            )
        }
        pendingTaskType = null
    }

    BatchQueueScreen(
        state = state,
        selectedTaskType = selectedTaskType,
        onSelectedTaskTypeChanged = { selectedTaskType = it },
        onPickInputsForTask = { taskType ->
            pendingTaskType = taskType
            if (taskType.allowsMultipleInputs) {
                multiPicker.launch(taskType.mimeFilter)
            } else {
                singlePicker.launch(taskType.mimeFilter)
            }
        },
        onRunQueue = viewModel::runQueue,
        onCancelQueue = viewModel::cancelQueue,
        onClearQueue = viewModel::clearQueue,
        onRemoveTask = viewModel::removeTask,
        onMoveTaskUp = viewModel::moveTaskUp,
        onMoveTaskDown = viewModel::moveTaskDown,
        onUpdateOutputBaseName = viewModel::updateOutputBaseName,
        presets = presets,
        presetName = presetName,
        onPresetNameChanged = { presetName = it },
        onSavePreset = { viewModel.saveQueuedTasksAsPreset(presetName) },
        onLoadPreset = viewModel::loadPreset,
        onDeletePreset = viewModel::deletePreset,
        onDismissError = viewModel::clearError,
        paddingValues = paddingValues
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun BatchQueueScreen(
    state: BatchQueueUiState,
    selectedTaskType: BatchTaskType,
    onSelectedTaskTypeChanged: (BatchTaskType) -> Unit,
    onPickInputsForTask: (BatchTaskType) -> Unit,
    onRunQueue: () -> Unit,
    onCancelQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onRemoveTask: (Long) -> Unit,
    onMoveTaskUp: (Long) -> Unit,
    onMoveTaskDown: (Long) -> Unit,
    onUpdateOutputBaseName: (Long, String) -> Unit,
    presets: List<BatchQueuePreset>,
    presetName: String,
    onPresetNameChanged: (String) -> Unit,
    onSavePreset: () -> Unit,
    onLoadPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onDismissError: () -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Batch Queue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Queue mixed offline operations and run them sequentially.")

        Text("Task Type", fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BatchTaskType.entries.forEach { type ->
                Button(onClick = { onSelectedTaskTypeChanged(type) }) {
                    val selected = if (type == selectedTaskType) "*" else ""
                    Text("${type.title}$selected")
                }
            }
        }

        Text(selectedTaskType.description, style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onPickInputsForTask(selectedTaskType) }) {
                Text("Add Task")
            }
            Button(onClick = onRunQueue, enabled = !state.isProcessing) {
                Text("Run Queue")
            }
            Button(onClick = onCancelQueue, enabled = state.isProcessing) {
                Text("Cancel")
            }
            Button(onClick = onClearQueue, enabled = !state.isProcessing) {
                Text("Clear")
            }
        }

        Text(
            "Queued: ${state.tasks.count { it.status == BatchTaskStatus.QUEUED }} | " +
                "Done: ${state.processedCount} | " +
                "Success: ${state.successCount} | " +
                "Failed: ${state.failureCount}",
            style = MaterialTheme.typography.bodySmall
        )

        Text("Task Presets", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = presetName,
            onValueChange = onPresetNameChanged,
            label = { Text("Preset name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing
        )
        Button(
            onClick = onSavePreset,
            enabled = !state.isProcessing && state.tasks.any { it.status == BatchTaskStatus.QUEUED }
        ) {
            Text("Save Queued As Preset")
        }

        if (presets.isEmpty()) {
            Text("No presets saved yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(preset.name, fontWeight = FontWeight.Medium)
                            Text("${preset.tasks.size} task(s)", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onLoadPreset(preset.id) }, enabled = !state.isProcessing) {
                                    Text("Load")
                                }
                                Button(onClick = { onDeletePreset(preset.id) }, enabled = !state.isProcessing) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onDismissError) { Text("Dismiss Error") }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val queuedIds = state.tasks
                .filter { task -> task.status == BatchTaskStatus.QUEUED }
                .map { task -> task.id }
            items(items = state.tasks, key = { task -> task.id }) { task ->
                val queuedIndex = queuedIds.indexOf(task.id)
                TaskCard(
                    task = task,
                    canRemove = !state.isProcessing || task.status != BatchTaskStatus.RUNNING,
                    canMoveUp = queuedIndex > 0,
                    canMoveDown = queuedIndex >= 0 && queuedIndex < queuedIds.lastIndex,
                    onRemove = { onRemoveTask(task.id) },
                    onMoveUp = { onMoveTaskUp(task.id) },
                    onMoveDown = { onMoveTaskDown(task.id) },
                    onSaveOutputBase = { outputBase -> onUpdateOutputBaseName(task.id, outputBase) }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: BatchQueueTask,
    canRemove: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSaveOutputBase: (String) -> Unit
) {
    var outputBaseDraft by remember(task.id, task.outputBaseName) { mutableStateOf(task.outputBaseName) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("#${task.id} ${task.type.title}", fontWeight = FontWeight.SemiBold)
            Text("Status: ${task.status.name}", style = MaterialTheme.typography.bodySmall)
            Text("Input: ${task.inputSummary}", style = MaterialTheme.typography.bodySmall)
            Text("Output base: ${task.outputBaseName}", style = MaterialTheme.typography.bodySmall)
            task.outputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
            task.outputSizeBytes?.let { Text("Size: $it bytes", style = MaterialTheme.typography.bodySmall) }
            task.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            if (task.status == BatchTaskStatus.QUEUED) {
                OutlinedTextField(
                    value = outputBaseDraft,
                    onValueChange = { outputBaseDraft = it },
                    label = { Text("Output base name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onMoveUp, enabled = canMoveUp) {
                        Text("Move Up")
                    }
                    Button(onClick = onMoveDown, enabled = canMoveDown) {
                        Text("Move Down")
                    }
                    Button(onClick = { onSaveOutputBase(outputBaseDraft) }) {
                        Text("Save Name")
                    }
                }
            }

            Button(onClick = onRemove, enabled = canRemove) {
                Text(if (task.status == BatchTaskStatus.QUEUED) "Remove" else "Remove Result")
            }
        }
    }
}

private fun readSourceLabel(context: Context, uri: Uri): String {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment ?: uri.toString()
}

private fun defaultPresetName(): String = "Preset_${System.currentTimeMillis()}"
