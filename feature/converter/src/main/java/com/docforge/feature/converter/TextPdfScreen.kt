package com.docforge.feature.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
fun TextPdfRoute(
    viewModel: TextPdfViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    TextPdfScreen(
        state = state,
        paddingValues = paddingValues,
        onTitleChanged = viewModel::onTitleChanged,
        onTextChanged = viewModel::onTextChanged,
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onPageSizeChanged = viewModel::onPageSizeChanged,
        onFormatModeChanged = viewModel::onFormatModeChanged,
        onConvert = viewModel::convertToPdf,
        onClearError = viewModel::clearError
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TextPdfScreen(
    state: TextPdfUiState,
    paddingValues: PaddingValues,
    onTitleChanged: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onPageSizeChanged: (PdfPageSize) -> Unit,
    onFormatModeChanged: (TextPdfFormatMode) -> Unit,
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
        Text("Text to PDF", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Paste or type text and instantly export a clean offline PDF.")

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            label = { Text("Document Title (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing
        )

        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChanged,
            label = { Text("Text Content") },
            minLines = 10,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing
        )

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing
        )

        Text("Page Size", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(PdfPageSize.A4, PdfPageSize.LETTER, PdfPageSize.LEGAL).forEach { size ->
                AssistChip(
                    onClick = { onPageSizeChanged(size) },
                    enabled = !state.isProcessing,
                    label = { Text(size.name) },
                    trailingIcon = if (state.pageSize == size) {
                        { Text("ON") }
                    } else {
                        null
                    }
                )
            }
        }

        Text("Formatting Mode", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextPdfFormatMode.entries.forEach { mode ->
                AssistChip(
                    onClick = { onFormatModeChanged(mode) },
                    enabled = !state.isProcessing,
                    label = { Text(mode.name) },
                    trailingIcon = if (state.formatMode == mode) {
                        { Text("ON") }
                    } else {
                        null
                    }
                )
            }
        }

        Button(
            onClick = onConvert,
            enabled = !state.isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Create PDF")
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
        state.lastParagraphCount?.let { Text("Paragraph lines: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastCharacterCount?.let { Text("Characters: $it", style = MaterialTheme.typography.bodySmall) }
    }
}
