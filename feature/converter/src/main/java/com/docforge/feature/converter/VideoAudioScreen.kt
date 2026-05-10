package com.docforge.feature.converter

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VideoAudioRoute(
    viewModel: VideoAudioViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    prefillUris: List<Uri> = emptyList(),
    onPrefillConsumed: () -> Unit = {}
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val label = readVideoLabel(context, uri)
            viewModel.onInputSelected(uri, label)
        }
    }
    LaunchedEffect(prefillUris) {
        val uri = prefillUris.firstOrNull() ?: return@LaunchedEffect
        val label = readVideoLabel(context, uri)
        viewModel.onInputSelected(uri, label)
        onPrefillConsumed()
    }

    VideoAudioScreen(
        state = state,
        paddingValues = paddingValues,
        onPickVideo = { picker.launch("video/*") },
        onOutputBaseNameChanged = viewModel::onOutputBaseNameChanged,
        onOutputFormatChanged = viewModel::onOutputFormatChanged,
        onExtractAudio = viewModel::extractAudio,
        onClearError = viewModel::clearError
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VideoAudioScreen(
    state: VideoAudioUiState,
    paddingValues: PaddingValues,
    onPickVideo: () -> Unit,
    onOutputBaseNameChanged: (String) -> Unit,
    onOutputFormatChanged: (AudioOutputFormat) -> Unit,
    onExtractAudio: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.VideoFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("Video to Audio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Extract audio tracks locally using stream demux. No upload, no re-encode.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickVideo)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    Text("Upload Video", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Tap to browse files", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Text("OUTPUT SETTINGS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                OutlinedTextField(
                    value = state.outputBaseName,
                    onValueChange = onOutputBaseNameChanged,
                    label = { Text("Output Base Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Output Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioOutputFormat.entries.forEach { format ->
                        FilterChip(
                            selected = state.outputFormat == format,
                            onClick = { onOutputFormatChanged(format) },
                            label = { Text(format.name) },
                            enabled = !state.isProcessing
                        )
                    }
                }
                
                if (state.outputFormat == AudioOutputFormat.MP3) {
                    Text("MP3 mode is passthrough-only: source must already contain an MP3 audio track.", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (state.selectedUri != null) "SELECTED FILES (1)" else "SELECTED FILES (0)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            if (state.inputLabel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DragIndicator, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.inputLabel, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${state.sourceMimeType ?: "Unknown format"} • ${state.durationMs?.let { formatDuration(it) } ?: "Unknown duration"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text("No files selected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rotation by animateFloatAsState(
                targetValue = if (state.isProcessing) 360f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing)
            )

            Button(
                onClick = onExtractAudio,
                enabled = !state.isProcessing && state.selectedUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(64.dp).shadow(8.dp, CircleShape)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Extracting...", fontWeight = FontWeight.Black, fontSize = 18.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotation))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Extract Audio", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }

        if (state.isProcessing || state.statusMessage != null || state.errorMessage != null || state.lastOutputPath != null) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                }
                state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp) }
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onClearError, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Dismiss Error") }
                }
                state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
                state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
            }
        }
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
    }
}

private fun readVideoLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}