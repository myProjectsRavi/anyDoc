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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.pdf.PdfPageSize

@Composable
fun PdfIdCardRoute(
    viewModel: PdfIdCardViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current

    val frontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onFrontSelected(uri, readLabel(context, uri))
    }

    val backPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onBackSelected(uri, readLabel(context, uri))
    }

    PdfIdCardScreen(
        state = state,
        paddingValues = paddingValues,
        onPickFront = { frontPicker.launch("image/*") },
        onPickBack = { backPicker.launch("image/*") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onPageSizeChanged = viewModel::onPageSizeChanged,
        onExport = viewModel::exportIdSheet,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfIdCardScreen(
    state: PdfIdCardUiState,
    paddingValues: PaddingValues,
    onPickFront: () -> Unit,
    onPickBack: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onPageSizeChanged: (PdfPageSize) -> Unit,
    onExport: () -> Unit,
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
        Text("ID Card / Passport Mode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Scan/select front + back and auto-compose both on one printable A4/Letter sheet.")

        Button(onClick = onPickFront, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.frontUri == null) "Select Front" else "Replace Front")
        }
        state.frontLabel?.let { Text("Front: $it", style = MaterialTheme.typography.bodySmall) }

        Button(onClick = onPickBack, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.backUri == null) "Select Back" else "Replace Back")
        }
        state.backLabel?.let { Text("Back: $it", style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(PdfPageSize.A4, PdfPageSize.LETTER).forEach { size ->
                val selected = if (state.pageSize == size) "*" else ""
                Button(onClick = { onPageSizeChanged(size) }, enabled = !state.isProcessing) {
                    Text("${size.name}$selected")
                }
            }
        }

        Button(onClick = onExport, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Export ID Sheet PDF")
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

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
