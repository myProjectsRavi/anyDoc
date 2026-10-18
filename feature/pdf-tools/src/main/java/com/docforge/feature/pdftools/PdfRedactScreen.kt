package com.docforge.feature.pdftools

import android.content.Context
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        onAutoDetectPiiChanged = viewModel::onAutoDetectPiiChanged,
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
    onAutoDetectPiiChanged: (Boolean) -> Unit,
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
            .padding(top = paddingValues.calculateTopPadding())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title & Subtitle
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("Redact & Merge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Securely remove sensitive data and combine your files into a single master document.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Left Column: Drop Zone & Settings
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Drop Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickPdf)
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
                    Text("Upload Documents", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Tap to browse files", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("Supports PDF", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // Tools Bento / Settings
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
                    Text("REDACTION RULES", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                CustomSwitchRow("Auto-detect emails / phones / SSNs", state.autoDetectPii, onAutoDetectPiiChanged)
                CustomSwitchRow("Case sensitive match", state.caseSensitive, onCaseSensitiveChanged)
                CustomSwitchRow("Scrub metadata", state.scrubMetadata, onScrubMetadataChanged)
                CustomSwitchRow("Scrub form field values", state.scrubFormValues, onScrubFormValuesChanged)
                CustomSwitchRow("Verify irreversible output", state.verifyIrreversible, onVerifyIrreversibleChanged)

                OutlinedTextField(
                    value = state.termsText,
                    onValueChange = onTermsTextChanged,
                    label = { Text("Terms to redact (one per line)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.outputName,
                    onValueChange = onOutputNameChanged,
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Right Column: Document List
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.inputLabel != null) "SELECTED FILES (1)" else "SELECTED FILES (0)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (state.inputLabel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DragIndicator, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.inputLabel, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("PDF Document", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { /* clear selected file, maybe add to viewmodel if needed, otherwise ignore */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                Text("No files selected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }

        // Process Button Section
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rotation by animateFloatAsState(
                targetValue = if (state.isProcessing) 360f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing)
            )

            Button(
                onClick = onRunRedaction,
                enabled = !state.isProcessing && state.inputLabel != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing...", fontWeight = FontWeight.Black, fontSize = 18.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotation))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Redact & Merge Now", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            Text(
                "By clicking process, you agree to our privacy terms. Documents are processed locally on your device for maximum security.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        // Status & Progress
        if (state.isProcessing || progress != null || state.statusMessage != null || state.errorMessage != null || state.lastOutputPath != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.progressStage?.let { stage ->
                    Text(stage, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                progress?.let { value ->
                    LinearProgressIndicator(
                        progress = { value.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp) }
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onClearError, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Dismiss Error")
                    }
                }
                state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
                state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
                state.removedTextOperatorCount?.let { Text("Removed text operators: $it", style = MaterialTheme.typography.bodySmall) }
                state.clearedFormFieldCount?.let { Text("Cleared form values: $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
        
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}