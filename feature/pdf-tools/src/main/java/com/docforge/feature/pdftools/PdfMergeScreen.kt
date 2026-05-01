package com.docforge.feature.pdftools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.pdf.PdfMergePageSizeMode
import com.docforge.core.ui.model.StableUriRef

@Composable
fun PdfMergeRoute(
    viewModel: PdfToolsViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    prefillUris: List<Uri> = emptyList(),
    onPrefillConsumed: () -> Unit = {}
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.onFilesSelected(uris)
    }
    LaunchedEffect(prefillUris) {
        if (prefillUris.isNotEmpty()) {
            viewModel.onFilesSelected(prefillUris)
            onPrefillConsumed()
        }
    }

    PdfMergeScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdfs = { picker.launch("*/*") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onMergeTitleChanged = viewModel::onMergeTitleChanged,
        onMergeAuthorChanged = viewModel::onMergeAuthorChanged,
        onMergeSubjectChanged = viewModel::onMergeSubjectChanged,
        onToggleBookmarks = { viewModel.onAddBookmarksChanged(!state.addBookmarks) },
        onMergePageSizeModeChanged = viewModel::onMergePageSizeModeChanged,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
        onRemove = viewModel::remove,
        onMerge = viewModel::mergeSelectedPdfs,
        onClearError = viewModel::clearError
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PdfMergeScreen(
    state: PdfToolsUiState,
    paddingValues: PaddingValues,
    onPickPdfs: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onMergeTitleChanged: (String) -> Unit,
    onMergeAuthorChanged: (String) -> Unit,
    onMergeSubjectChanged: (String) -> Unit,
    onToggleBookmarks: () -> Unit,
    onMergePageSizeModeChanged: (PdfMergePageSizeMode) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMerge: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mixed Merge", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Combine PDFs and images into one offline PDF file.")

        Button(onClick = onPickPdfs, modifier = Modifier.fillMaxWidth(), enabled = !state.isMerging) {
            Text(if (state.selectedUris.isEmpty()) "Select PDFs / Images" else "Replace Sources")
        }

        Text("Selected merge sources: ${state.selectedUris.size} file(s)")

        if (state.selectedUris.isNotEmpty()) {
            OutlinedTextField(
                value = state.outputName,
                onValueChange = onOutputNameChanged,
                label = { Text("Output File Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.mergeTitle,
                onValueChange = onMergeTitleChanged,
                label = { Text("PDF Title (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.mergeAuthor,
                onValueChange = onMergeAuthorChanged,
                label = { Text("PDF Author (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.mergeSubject,
                onValueChange = onMergeSubjectChanged,
                label = { Text("PDF Subject (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(onClick = onToggleBookmarks, enabled = !state.isMerging) {
                Text(if (state.addBookmarks) "Source Bookmarks: ON" else "Source Bookmarks: OFF")
            }
            Text("Page Size Normalization")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PdfMergePageSizeMode.entries.forEach { mode ->
                    Button(
                        onClick = { onMergePageSizeModeChanged(mode) },
                        enabled = !state.isMerging
                    ) {
                        Text(
                            when (mode) {
                                PdfMergePageSizeMode.KEEP_SOURCE -> if (state.mergePageSizeMode == mode) "Keep Source ON" else "Keep Source"
                                PdfMergePageSizeMode.A4_FIT -> if (state.mergePageSizeMode == mode) "Fit A4 ON" else "Fit A4"
                                PdfMergePageSizeMode.LETTER_FIT -> if (state.mergePageSizeMode == mode) "Fit Letter ON" else "Fit Letter"
                            }
                        )
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(state.selectedUris) { index, uriRef ->
                    PdfItemCard(
                        index = index,
                        uriRef = uriRef,
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onRemove = { onRemove(index) },
                        moveUpEnabled = index > 0,
                        moveDownEnabled = index < state.selectedUris.lastIndex,
                        enabled = !state.isMerging
                    )
                }
            }
        }

        Button(
            onClick = onMerge,
            enabled = !state.isMerging,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isMerging) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Merge to PDF")
            }
        }

        state.statusMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.tertiary)
        }

        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) {
                Text("Dismiss Error")
            }
        }

        state.lastOutputPath?.let { path ->
            Text("Saved: $path", style = MaterialTheme.typography.bodySmall)
            Text("Size: ${state.lastOutputSizeBytes ?: 0} bytes", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PdfItemCard(
    index: Int,
    uriRef: StableUriRef,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    moveUpEnabled: Boolean,
    moveDownEnabled: Boolean,
    enabled: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uri = remember(uriRef) { uriRef.toUri() }
    val label = remember(uriRef) {
        DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment ?: uri.toString()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${index + 1}. $label", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMoveUp, enabled = enabled && moveUpEnabled) { Text("Up") }
                Button(onClick = onMoveDown, enabled = enabled && moveDownEnabled) { Text("Down") }
                Button(onClick = onRemove, enabled = enabled) { Text("Remove") }
            }
        }
    }
}
