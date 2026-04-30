package com.docforge.feature.converter

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Video to Audio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Extract audio tracks locally using stream demux. No upload, no re-encode.")

        Button(onClick = onPickVideo, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select Video" else "Replace Video")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }
        state.sourceMimeType?.let { Text("Source audio: $it", style = MaterialTheme.typography.bodySmall) }
        state.durationMs?.let { Text("Duration: ${formatDuration(it)}", style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.outputBaseName,
            onValueChange = onOutputBaseNameChanged,
            label = { Text("Output Base Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Output Format", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioOutputFormat.entries.forEach { format ->
                Button(
                    onClick = { onOutputFormatChanged(format) },
                    enabled = !state.isProcessing
                ) {
                    val selected = if (state.outputFormat == format) "*" else ""
                    Text("${format.name}$selected")
                }
            }
        }

        if (state.outputFormat == AudioOutputFormat.MP3) {
            Text(
                "MP3 mode is passthrough-only: source must already contain an MP3 audio track.",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(onClick = onExtractAudio, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Extract Audio")
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }
        state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
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
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
