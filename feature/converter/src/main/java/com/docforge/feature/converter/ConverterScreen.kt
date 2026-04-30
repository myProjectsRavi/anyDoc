package com.docforge.feature.converter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.pdf.PdfPageSize

@Composable
fun ConverterRoute(
    viewModel: ConverterViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.onImagesSelected(uris)
    }

    ConverterScreen(
        state = state,
        paddingValues = paddingValues,
        onPickImages = { launcher.launch("image/*") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onPageSizeChanged = viewModel::onPageSizeChanged,
        onConvert = viewModel::convertImagesToPdf,
        onClearError = viewModel::clearError
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ConverterScreen(
    state: ConverterUiState,
    paddingValues: PaddingValues,
    onPickImages: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onPageSizeChanged: (PdfPageSize) -> Unit,
    onConvert: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Images to PDF",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Offline conversion. Your files never leave your phone.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onPickImages,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.selectedUris.isEmpty()) "Select Images" else "Replace Selection")
        }

        if (state.selectedUris.isNotEmpty()) {
            Text(
                text = "Selected: ${state.selectedUris.size} image(s)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Page Size", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PdfPageSize.entries.forEach { size ->
                AssistChip(
                    onClick = { onPageSizeChanged(size) },
                    label = { Text(size.name) },
                    enabled = !state.isConverting,
                    trailingIcon = if (size == state.pageSize) {
                        { Text("ON") }
                    } else {
                        null
                    }
                )
            }
        }

        Button(
            onClick = onConvert,
            enabled = !state.isConverting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isConverting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp)
                )
            }
            Text(if (state.isConverting) "Converting..." else "Convert to PDF")
        }

        state.statusMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        state.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onClearError) {
                Text("Dismiss Error")
            }
        }

        state.lastOutputPath?.let { path ->
            Text(
                text = "Saved: $path",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Size: ${state.lastOutputSizeBytes ?: 0} bytes",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
