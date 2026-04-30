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
fun PdfRedactRoute(
    viewModel: PdfRedactViewModel,
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

    PdfRedactScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onTermsTextChanged = viewModel::onTermsTextChanged,
        onCaseSensitiveChanged = viewModel::onCaseSensitiveChanged,
        onScrubMetadataChanged = viewModel::onScrubMetadataChanged,
        onScrubFormValuesChanged = viewModel::onScrubFormValuesChanged,
        onVerifyIrreversibleChanged = viewModel::onVerifyIrreversibleChanged,
        onRunRedaction = viewModel::redact,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfRedactScreen(
    state: PdfRedactUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onTermsTextChanged: (String) -> Unit,
    onCaseSensitiveChanged: (Boolean) -> Unit,
    onScrubMetadataChanged: (Boolean) -> Unit,
    onScrubFormValuesChanged: (Boolean) -> Unit,
    onVerifyIrreversibleChanged: (Boolean) -> Unit,
    onRunRedaction: () -> Unit,
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
        Text("True PDF Redaction", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Removes matching text operators from PDF content streams and verifies output before saving.")

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
            value = state.termsText,
            onValueChange = onTermsTextChanged,
            label = { Text("Terms to redact (one per line)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Case sensitive match")
            Switch(checked = state.caseSensitive, onCheckedChange = onCaseSensitiveChanged)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Scrub metadata")
            Switch(checked = state.scrubMetadata, onCheckedChange = onScrubMetadataChanged)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Scrub form field values")
            Switch(checked = state.scrubFormValues, onCheckedChange = onScrubFormValuesChanged)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Verify irreversible output")
            Switch(checked = state.verifyIrreversible, onCheckedChange = onVerifyIrreversibleChanged)
        }

        Button(onClick = onRunRedaction, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Run Redaction")
            }
        }

        state.progressStage?.let { stage ->
            Text(stage, style = MaterialTheme.typography.bodySmall)
        }
        progress?.let { value ->
            LinearProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }
        state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
        state.removedTextOperatorCount?.let { Text("Removed text operators: $it", style = MaterialTheme.typography.bodySmall) }
        state.clearedFormFieldCount?.let { Text("Cleared form values: $it", style = MaterialTheme.typography.bodySmall) }
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
