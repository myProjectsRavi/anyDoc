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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
fun PdfOcrRoute(
    viewModel: PdfOcrViewModel,
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

    PdfOcrScreen(
        state = state,
        paddingValues = paddingValues,
        onPickInput = { picker.launch("*/*") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onCreateSearchablePdfChanged = viewModel::onCreateSearchablePdfChanged,
        onRunOcr = viewModel::runOcr,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfOcrScreen(
    state: PdfOcrUiState,
    paddingValues: PaddingValues,
    onPickInput: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onCreateSearchablePdfChanged: (Boolean) -> Unit,
    onRunOcr: () -> Unit,
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
        Text("Offline OCR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Extract text from scanned PDFs/images and generate searchable PDFs fully offline.")

        Button(onClick = onPickInput, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF or Image" else "Replace Input")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output Base Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Create searchable PDF")
            Switch(
                checked = state.createSearchablePdf,
                onCheckedChange = onCreateSearchablePdfChanged
            )
        }

        Button(onClick = onRunOcr, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Run OCR")
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

        state.textOutputPath?.let { Text("Text: $it", style = MaterialTheme.typography.bodySmall) }
        state.searchablePdfPath?.let { Text("Searchable PDF: $it", style = MaterialTheme.typography.bodySmall) }
        state.extractedChars?.let { Text("Chars: $it", style = MaterialTheme.typography.bodySmall) }
        state.lineCount?.let { Text("Lines: $it", style = MaterialTheme.typography.bodySmall) }
        state.extractedTextPreview?.takeIf { it.isNotBlank() }?.let { preview ->
            Text("Extracted text preview", fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Text(preview, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
