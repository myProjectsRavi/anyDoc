package com.docforge.feature.pdftools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(top = paddingValues.calculateTopPadding())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title & Subtitle
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Merge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("Mixed Merge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Combine PDFs and images into one offline PDF file.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // LazyColumn for scrollable content (Settings + List + Button)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Drop Zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable(onClick = onPickPdfs)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                        Text(if (state.selectedUris.isEmpty()) "Select PDFs & Images" else "Add More Files", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Supports PDF, JPG, PNG", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            if (state.selectedUris.isNotEmpty()) {
                item {
                    // Settings Bento
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Text("MERGE SETTINGS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }

                        OutlinedTextField(value = state.outputName, onValueChange = onOutputNameChanged, label = { Text("Output File Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.mergeTitle, onValueChange = onMergeTitleChanged, label = { Text("PDF Title (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.mergeAuthor, onValueChange = onMergeAuthorChanged, label = { Text("PDF Author (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.mergeSubject, onValueChange = onMergeSubjectChanged, label = { Text("PDF Subject (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                        CustomSwitchRow("Include Source Bookmarks", state.addBookmarks, { onToggleBookmarks() })

                        Text("Page Size Normalization", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PdfMergePageSizeMode.entries.forEach { mode ->
                                val selected = state.mergePageSizeMode == mode
                                Button(
                                    onClick = { onMergePageSizeModeChanged(mode) },
                                    enabled = !state.isMerging,
                                    colors = if (selected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) 
                                             else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text(
                                        when (mode) {
                                            PdfMergePageSizeMode.KEEP_SOURCE -> "Keep Source"
                                            PdfMergePageSizeMode.A4_FIT -> "Fit A4"
                                            PdfMergePageSizeMode.LETTER_FIT -> "Fit Letter"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "SELECTED FILES (${state.selectedUris.size})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

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

            item {
                // Process Button
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                    val rotation by animateFloatAsState(targetValue = if (state.isMerging) 360f else 0f, animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing))

                    Button(
                        onClick = onMerge,
                        enabled = !state.isMerging && state.selectedUris.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(64.dp).shadow(8.dp, CircleShape)
                    ) {
                        if (state.isMerging) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Processing...", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotation))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Merge to PDF", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }

            item {
                // Status & Progress
                if (state.isMerging || state.statusMessage != null || state.errorMessage != null || state.lastOutputPath != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp) }
                        state.errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = onClearError, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("Dismiss Error")
                            }
                        }
                        state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
                        state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedThumbColor = Color.White, uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant)
        )
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.DragIndicator, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${index + 1}. $label", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text("Document / Image", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onMoveUp, enabled = enabled && moveUpEnabled) { Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = if (moveUpEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant) }
            IconButton(onClick = onMoveDown, enabled = enabled && moveDownEnabled) { Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = if (moveDownEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant) }
            IconButton(onClick = onRemove, enabled = enabled) { Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) }
        }
    }
}
