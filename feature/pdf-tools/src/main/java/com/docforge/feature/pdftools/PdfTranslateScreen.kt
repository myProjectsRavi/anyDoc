package com.docforge.feature.pdftools

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PdfTranslateRoute(
    viewModel: PdfTranslateViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onInputSelected(uri, readLabel(context, uri))
        }
    }

    PdfTranslateScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onSourceLanguageChanged = viewModel::onSourceLanguageChanged,
        onTargetLanguageChanged = viewModel::onTargetLanguageChanged,
        onTranslate = viewModel::translate,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfTranslateScreen(
    state: PdfTranslateUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onSourceLanguageChanged: (String) -> Unit,
    onTargetLanguageChanged: (String) -> Unit,
    onTranslate: () -> Unit,
    onClearError: () -> Unit
) {
    val progress = if (state.progressTotal > 0) {
        state.progressCurrent.toFloat() / state.progressTotal.toFloat()
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Auto-Translate PDF", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Translate PDF text offline with line-level layout-preserving overlays (Android 12+ on-device translation).")

        Button(onClick = onPickPdf, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF" else "Replace PDF")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.sourceLanguageTag,
            onValueChange = onSourceLanguageChanged,
            label = { Text("Source Language Tag (e.g. en)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.targetLanguageTag,
            onValueChange = onTargetLanguageChanged,
            label = { Text("Target Language Tag (e.g. es)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = onTranslate, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Translate PDF")
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.progressStage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        progress?.let { value ->
            LinearProgressIndicator(
                progress = { value.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }
        state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
        state.translatedLineCount?.let { Text("Translated lines: $it", style = MaterialTheme.typography.bodySmall) }
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
