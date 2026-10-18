package com.docforge.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Defaults Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("DEFAULTS", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Text("Default PDF Page Size", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PdfPageSize.entries.forEach { size ->
                    val isSelected = state.defaultPdfPageSize == size
                    Button(
                        onClick = { onPdfPageSizeChanged(size) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(size.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Default PDF Compression", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PdfCompressionLevel.entries.forEach { level ->
                    val isSelected = state.defaultPdfCompressionLevel == level
                    Button(
                        onClick = { onPdfCompressionChanged(level) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(level.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.defaultImageQualityInput,
                onValueChange = onDefaultImageQualityChanged,
                label = { Text("Default image quality (10-100)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = onApplyDefaultImageQuality,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Apply Image Quality Default", fontWeight = FontWeight.Bold)
            }
        }

        // Output Folders Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("OUTPUT FOLDERS", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text("Folders are app-local under Android media directories.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

            OutlinedTextField(
                value = state.documentsFolderNameInput,
                onValueChange = onDocumentsFolderNameChanged,
                label = { Text("Documents folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.imagesFolderNameInput,
                onValueChange = onImagesFolderNameChanged,
                label = { Text("Images folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.audioFolderNameInput,
                onValueChange = onAudioFolderNameChanged,
                label = { Text("Audio folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = onSaveOutputFolders,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Save Output Folders", fontWeight = FontWeight.Bold)
            }
        }

        state.statusMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp)
            )
        }
        state.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
