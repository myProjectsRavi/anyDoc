package com.docforge.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.pdf.PdfCompressionLevel
import com.docforge.core.pdf.PdfPageSize

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    SettingsScreen(
        state = state,
        paddingValues = paddingValues,
        onPdfPageSizeChanged = viewModel::onPdfPageSizeChanged,
        onPdfCompressionChanged = viewModel::onPdfCompressionChanged,
        onDefaultImageQualityChanged = viewModel::onDefaultImageQualityChanged,
        onApplyDefaultImageQuality = viewModel::applyDefaultImageQuality,
        onDocumentsFolderNameChanged = viewModel::onDocumentsFolderNameChanged,
        onImagesFolderNameChanged = viewModel::onImagesFolderNameChanged,
        onAudioFolderNameChanged = viewModel::onAudioFolderNameChanged,
        onSaveOutputFolders = viewModel::saveOutputFolders,
        onBack = onBack
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    paddingValues: PaddingValues,
    onPdfPageSizeChanged: (PdfPageSize) -> Unit,
    onPdfCompressionChanged: (PdfCompressionLevel) -> Unit,
    onDefaultImageQualityChanged: (String) -> Unit,
    onApplyDefaultImageQuality: () -> Unit,
    onDocumentsFolderNameChanged: (String) -> Unit,
    onImagesFolderNameChanged: (String) -> Unit,
    onAudioFolderNameChanged: (String) -> Unit,
    onSaveOutputFolders: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Defaults", fontWeight = FontWeight.SemiBold)
                Text("Default PDF page size")
                PdfPageSize.entries.forEach { size ->
                    val marker = if (state.defaultPdfPageSize == size) "* " else ""
                    Button(
                        onClick = { onPdfPageSizeChanged(size) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$marker${size.name}")
                    }
                }

                Text("Default PDF compression")
                PdfCompressionLevel.entries.forEach { level ->
                    val marker = if (state.defaultPdfCompressionLevel == level) "* " else ""
                    Button(
                        onClick = { onPdfCompressionChanged(level) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$marker${level.name}")
                    }
                }

                OutlinedTextField(
                    value = state.defaultImageQualityInput,
                    onValueChange = onDefaultImageQualityChanged,
                    label = { Text("Default image quality (10-100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onApplyDefaultImageQuality,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Image Quality Default")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Default Output Folders", fontWeight = FontWeight.SemiBold)
                Text("Folders are app-local under Android media directories.")

                OutlinedTextField(
                    value = state.documentsFolderNameInput,
                    onValueChange = onDocumentsFolderNameChanged,
                    label = { Text("Documents folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.imagesFolderNameInput,
                    onValueChange = onImagesFolderNameChanged,
                    label = { Text("Images folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.audioFolderNameInput,
                    onValueChange = onAudioFolderNameChanged,
                    label = { Text("Audio folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onSaveOutputFolders,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Output Folders")
                }
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
