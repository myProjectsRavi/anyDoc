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
import com.docforge.core.pdf.PdfCompressionLevel

@Composable
fun PdfCompressRoute(
    viewModel: PdfCompressViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val labelAndSize = readLabelAndSize(context, uri)
            viewModel.onInputSelected(uri, labelAndSize.first, labelAndSize.second)
        }
    }

    PdfCompressScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onLevelChanged = viewModel::onLevelChanged,
        onCompress = viewModel::compress,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfCompressScreen(
    state: PdfCompressUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onLevelChanged: (PdfCompressionLevel) -> Unit,
    onCompress: () -> Unit,
    onClearError: () -> Unit
) {
    val estimate = state.inputSizeBytes?.let { size ->
        (size * state.compressionLevel.estimatedRatio).toLong()
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
        Text("PDF Compress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Reduce PDF file size with offline quality presets.")

        Button(onClick = onPickPdf, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF" else "Replace PDF")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }
        state.pageCount?.let { Text("Pages: $it", style = MaterialTheme.typography.bodySmall) }
        state.inputSizeBytes?.let { Text("Input size: $it bytes", style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Compression Level", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PdfCompressionLevel.entries.forEach { level ->
                Button(
                    onClick = { onLevelChanged(level) },
                    enabled = !state.isProcessing
                ) {
                    val selected = if (level == state.compressionLevel) "*" else ""
                    Text("${level.name}$selected")
                }
            }
        }

        estimate?.let { Text("Estimated output: ~$it bytes", style = MaterialTheme.typography.bodySmall) }

        Button(onClick = onCompress, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Compress PDF")
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

private fun readLabelAndSize(context: Context, uri: Uri): Pair<String?, Long?> {
    val doc = DocumentFile.fromSingleUri(context, uri)
    return (doc?.name ?: uri.lastPathSegment) to doc?.length()
}
