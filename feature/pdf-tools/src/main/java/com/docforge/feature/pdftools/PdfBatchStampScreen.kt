package com.docforge.feature.pdftools

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PdfBatchStampRoute(
    viewModel: PdfBatchStampViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val filtered = uris.filter { uri ->
            val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
            mime.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")
        }
        if (filtered.isNotEmpty()) {
            viewModel.onInputsSelected(filtered, filtered.map { readLabel(context, it) ?: it.toString() })
        }
    }

    PdfBatchStampScreen(
        state = state,
        paddingValues = paddingValues,
        onPickInputs = { picker.launch("application/pdf") },
        onOutputBaseNameChanged = viewModel::onOutputBaseNameChanged,
        onWatermarkTextChanged = viewModel::onWatermarkTextChanged,
        onEnableWatermarkChanged = viewModel::onEnableWatermarkChanged,
        onEnableBatesChanged = viewModel::onEnableBatesChanged,
        onBatesPrefixChanged = viewModel::onBatesPrefixChanged,
        onBatesStartChanged = viewModel::onBatesStartChanged,
        onBatesPaddingChanged = viewModel::onBatesPaddingChanged,
        onApply = viewModel::applyStamping,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfBatchStampScreen(
    state: PdfBatchStampUiState,
    paddingValues: PaddingValues,
    onPickInputs: () -> Unit,
    onOutputBaseNameChanged: (String) -> Unit,
    onWatermarkTextChanged: (String) -> Unit,
    onEnableWatermarkChanged: (Boolean) -> Unit,
    onEnableBatesChanged: (Boolean) -> Unit,
    onBatesPrefixChanged: (String) -> Unit,
    onBatesStartChanged: (String) -> Unit,
    onBatesPaddingChanged: (Int) -> Unit,
    onApply: () -> Unit,
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
        Text("Batch Watermark + Bates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Apply legal-grade watermark text and incremental Bates numbering across multiple PDFs.")

        Button(onClick = onPickInputs, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUris.isEmpty()) "Select PDFs" else "Replace PDFs (${state.selectedUris.size})")
        }

        if (state.inputLabels.isNotEmpty()) {
            state.inputLabels.forEachIndexed { index, label ->
                Text("${index + 1}. $label", style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedTextField(
            value = state.outputBaseName,
            onValueChange = onOutputBaseNameChanged,
            label = { Text("Output Base Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Watermark")
            Switch(checked = state.enableWatermark, onCheckedChange = onEnableWatermarkChanged)
        }
        OutlinedTextField(
            value = state.watermarkText,
            onValueChange = onWatermarkTextChanged,
            label = { Text("Watermark Text") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.enableWatermark
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bates Numbering")
            Switch(checked = state.enableBates, onCheckedChange = onEnableBatesChanged)
        }
        OutlinedTextField(
            value = state.batesPrefix,
            onValueChange = onBatesPrefixChanged,
            label = { Text("Bates Prefix") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.enableBates
        )
        OutlinedTextField(
            value = state.batesStart,
            onValueChange = onBatesStartChanged,
            label = { Text("Bates Start") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.enableBates
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onBatesPaddingChanged((state.batesPadding - 1).coerceAtLeast(1)) }, enabled = !state.isProcessing) {
                Text("Pad -")
            }
            Text("Padding: ${state.batesPadding}", modifier = Modifier.padding(top = 10.dp))
            Button(onClick = { onBatesPaddingChanged((state.batesPadding + 1).coerceAtMost(10)) }, enabled = !state.isProcessing) {
                Text("Pad +")
            }
        }

        Button(onClick = onApply, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Apply Batch Stamps")
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }
        state.lastOutputPaths.forEach { path -> Text(path, style = MaterialTheme.typography.bodySmall) }
        state.lastOutputSizeBytes?.let { Text("Total output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
