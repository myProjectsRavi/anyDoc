package com.docforge.app.batch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.runQueue()
        } else {
            viewModel.onNotificationPermissionDenied()
        }
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
        onRunQueue = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                viewModel.runQueue()
                return@BatchQueueScreen
            }
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasNotificationPermission) {
                viewModel.runQueue()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
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
            .padding(top = paddingValues.calculateTopPadding())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("Batch Queue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Queue mixed offline operations and run them sequentially.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Action Buttons Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (!state.isProcessing) {
                Button(
                    onClick = onRunQueue,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Queue", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onCancelQueue,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onClearQueue,
                enabled = !state.isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.ClearAll, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear", fontWeight = FontWeight.Bold)
            }
        }

        // Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem("Queued", state.tasks.count { it.status == BatchTaskStatus.QUEUED })
            StatItem("Done", state.processedCount)
            StatItem("Success", state.successCount, color = MaterialTheme.colorScheme.primary)
            StatItem("Failed", state.failureCount, color = MaterialTheme.colorScheme.error)
        }

        // Task Configuration Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("ADD TASK", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatchTaskType.entries.forEach { type ->
                    val isSelected = type == selectedTaskType
                    Button(
                        onClick = { onSelectedTaskTypeChanged(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(type.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Text(selectedTaskType.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { onPickInputsForTask(selectedTaskType) }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Text("Select Files & Add Task", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Presets Section
        if (presets.isNotEmpty() || state.tasks.any { it.status == BatchTaskStatus.QUEUED }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("PRESETS", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = onPresetNameChanged,
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isProcessing
                    )
                    Button(
                        onClick = onSavePreset,
                        enabled = !state.isProcessing && state.tasks.any { it.status == BatchTaskStatus.QUEUED },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                }

                if (presets.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column {
                                        Text(preset.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text("${preset.tasks.size} tasks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onLoadPreset(preset.id) }, enabled = !state.isProcessing, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Load", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onDeletePreset(preset.id) }, enabled = !state.isProcessing, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Messages
        state.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        }
        state.errorMessage?.let {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(16.dp)) {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismissError, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Dismiss")
                }
            }
        }

        // Tasks List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 80.dp)
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
private fun StatItem(label: String, value: Int, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    val statusColor = when (task.status) {
        BatchTaskStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
        BatchTaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
        BatchTaskStatus.SUCCESS -> MaterialTheme.colorScheme.secondary
        BatchTaskStatus.FAILED -> MaterialTheme.colorScheme.error
        BatchTaskStatus.CANCELED -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#${task.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(task.type.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(task.status.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = statusColor)
            }

            Text("Input: ${task.inputSummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            
            task.outputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
            task.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }

            if (task.status == BatchTaskStatus.QUEUED) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = outputBaseDraft,
                        onValueChange = { outputBaseDraft = it },
                        label = { Text("Output base name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(onClick = { onSaveOutputBase(outputBaseDraft) }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))) {
                        Icon(Icons.Default.Save, contentDescription = "Save Name", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up") }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down") }
                    IconButton(onClick = onRemove, enabled = canRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onRemove, enabled = canRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove Result") }
                }
            }
        }
    }
}

private fun readSourceLabel(context: Context, uri: Uri): String {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment ?: uri.toString()
}

private fun defaultPresetName(): String = "Preset_${System.currentTimeMillis()}"
