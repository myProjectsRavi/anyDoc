package com.docforge.feature.pdftools

import android.content.Context
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
import com.docforge.core.pdf.PdfPageImageFormat

@Composable
fun PdfPageImageRoute(
    viewModel: PdfPageImageViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val label = readDocLabel(context, uri)
            viewModel.onInputSelected(uri, label)
        }
    }

    PdfPageImageScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onOutputBaseNameChanged = viewModel::onOutputBaseNameChanged,
        onFormatChanged = viewModel::onFormatChanged,
        onZipBundleOutputChanged = viewModel::onZipBundleOutputChanged,
        onJpegQualityChanged = viewModel::onJpegQualityChanged,
        onScaleChanged = viewModel::onScaleChanged,
        onExportPages = viewModel::exportPages,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfPageImageScreen(
    state: PdfPageImageUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onOutputBaseNameChanged: (String) -> Unit,
    onFormatChanged: (PdfPageImageFormat) -> Unit,
    onZipBundleOutputChanged: (Boolean) -> Unit,
    onJpegQualityChanged: (String) -> Unit,
    onScaleChanged: (String) -> Unit,
    onExportPages: () -> Unit,
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
        Text("PDF to Images", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Export each PDF page as JPG/PNG/WebP images.")

        Button(onClick = onPickPdf, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF" else "Replace PDF")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }
        state.pageCount?.let { Text("Pages: $it", style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.outputBaseName,
            onValueChange = onOutputBaseNameChanged,
            label = { Text("Output Base Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Image Format", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PdfPageImageFormat.entries.forEach { format ->
                Button(
                    onClick = { onFormatChanged(format) },
                    enabled = !state.isProcessing
                ) {
                    val selected = if (state.imageFormat == format) "*" else ""
                    Text("${format.name}$selected")
                }
            }
        }

        Button(
            onClick = { onZipBundleOutputChanged(!state.zipBundleOutput) },
            enabled = !state.isProcessing
        ) {
            Text(if (state.zipBundleOutput) "ZIP Bundle: ON" else "ZIP Bundle: OFF")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.jpegQualityInput,
                onValueChange = onJpegQualityChanged,
                label = { Text("JPG/WebP Quality") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = state.scaleInput,
            onValueChange = onScaleChanged,
            label = { Text("Scale (0.25-2.0)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = onExportPages, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Export Pages")
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

private fun readDocLabel(context: Context, uri: android.net.Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
