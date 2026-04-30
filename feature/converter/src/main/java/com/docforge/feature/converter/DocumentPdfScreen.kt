package com.docforge.feature.converter

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
fun DocumentPdfRoute(
    viewModel: DocumentPdfViewModel,
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
            val label = readSourceLabel(context, uri)
            viewModel.onInputSelected(uri, label)
        }
    }
    LaunchedEffect(prefillUris) {
        val uri = prefillUris.firstOrNull() ?: return@LaunchedEffect
        val label = readSourceLabel(context, uri)
        viewModel.onInputSelected(uri, label)
        onPrefillConsumed()
    }

    DocumentPdfScreen(
        state = state,
        paddingValues = paddingValues,
        onPickDocument = { picker.launch("*/*") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onConvert = viewModel::convertToPdf,
        onClearError = viewModel::clearError
    )
}

@Composable
fun DocumentPdfScreen(
    state: DocumentPdfUiState,
    paddingValues: PaddingValues,
    onPickDocument: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
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
        Text("Document to PDF", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Convert DOCX, RTF, CSV, and TXT documents to PDF fully offline.")

        Button(onClick = onPickDocument, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select Document" else "Replace Document")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }
        Text("Detected type: ${state.inputType.name}", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = onConvert, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Convert to PDF")
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }
        state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
        state.lastPageCount?.let { Text("Pages: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastLineCount?.let { Text("Parsed lines: $it", style = MaterialTheme.typography.bodySmall) }
    }
}

private fun readSourceLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
