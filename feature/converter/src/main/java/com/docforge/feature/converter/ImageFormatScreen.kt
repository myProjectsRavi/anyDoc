package com.docforge.feature.converter

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ImageFormatRoute(
    viewModel: ImageFormatViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    prefillUris: List<Uri> = emptyList(),
    onPrefillConsumed: () -> Unit = {}
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.onImagesSelected(uris)
    }
    LaunchedEffect(prefillUris) {
        if (prefillUris.isNotEmpty()) {
            viewModel.onImagesSelected(prefillUris)
            onPrefillConsumed()
        }
    }

    ImageFormatScreen(
        state = state,
        paddingValues = paddingValues,
        onPickImages = { picker.launch("image/*") },
        onOutputBaseNameChanged = viewModel::onOutputBaseNameChanged,
        onOutputFormatChanged = viewModel::onOutputFormatChanged,
        onQualityChanged = viewModel::onQualityChanged,
        onScaleChanged = viewModel::onScaleChanged,
        onConvert = viewModel::convert,
        onClearError = viewModel::clearError
    )
}

@Composable
fun ImageFormatScreen(
    state: ImageFormatUiState,
    paddingValues: PaddingValues,
    onPickImages: () -> Unit,
    onOutputBaseNameChanged: (String) -> Unit,
    onOutputFormatChanged: (ImageOutputFormat) -> Unit,
    onQualityChanged: (String) -> Unit,
    onScaleChanged: (String) -> Unit,
    onConvert: () -> Unit,
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
        Text("Image Format Convert", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Batch convert HEIC/WebP/BMP/TIFF/JPG/PNG inputs into JPG/PNG/WebP outputs.")

        Button(onClick = onPickImages, enabled = !state.isConverting, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUris.isEmpty()) "Select Images" else "Replace Selection")
        }
        if (state.selectedUris.isNotEmpty()) {
            Text("Selected: ${state.selectedUris.size} image(s)")
        }

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
            ImageOutputFormat.entries.forEach { format ->
                Button(
                    onClick = { onOutputFormatChanged(format) },
                    enabled = !state.isConverting
                ) {
                    val selected = if (format == state.outputFormat) "*" else ""
                    Text("${format.name}$selected")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.qualityInput,
                onValueChange = onQualityChanged,
                label = { Text("Quality") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = state.scaleInput,
            onValueChange = onScaleChanged,
            label = { Text("Scale") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = onConvert, enabled = !state.isConverting, modifier = Modifier.fillMaxWidth()) {
            if (state.isConverting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Convert Images")
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }

        if (state.lastOutputPaths.isNotEmpty()) {
            Text("Outputs", fontWeight = FontWeight.SemiBold)
            state.lastOutputPaths.forEach { path ->
                Text(path, style = MaterialTheme.typography.bodySmall)
            }
        }
        state.lastOutputSizeBytes?.let { Text("Total output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
    }
}
