package com.docforge.feature.pdftools

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.LibraryAddCheck
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
import androidx.compose.runtime.getValue
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

@Composable
fun PdfBatchStampRoute(
    viewModel: PdfBatchStampViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val filtered = uris.filter { uri ->
            val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
            mime.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")
        }
        if (filtered.isNotEmpty()) {
            viewModel.onInputsSelected(filtered, filtered.map { readLabel(context, it) ?: it.toString() })
        }
    }

    PdfBatchStampScreen(
        state = state,
        paddingValues = paddingValues,
        onPickInputs = { picker.launch("application/pdf") },
        onOutputBaseNameChanged = viewModel::onOutputBaseNameChanged,
        onWatermarkTextChanged = viewModel::onWatermarkTextChanged,
        onEnableWatermarkChanged = viewModel::onEnableWatermarkChanged,
        onEnableBatesChanged = viewModel::onEnableBatesChanged,
        onBatesPrefixChanged = viewModel::onBatesPrefixChanged,
        onBatesStartChanged = viewModel::onBatesStartChanged,
        onBatesPaddingChanged = viewModel::onBatesPaddingChanged,
        onApply = viewModel::applyStamping,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfBatchStampScreen(
    state: PdfBatchStampUiState,
    paddingValues: PaddingValues,
    onPickInputs: () -> Unit,
    onOutputBaseNameChanged: (String) -> Unit,
    onWatermarkTextChanged: (String) -> Unit,
    onEnableWatermarkChanged: (Boolean) -> Unit,
    onEnableBatesChanged: (Boolean) -> Unit,
    onBatesPrefixChanged: (String) -> Unit,
    onBatesStartChanged: (String) -> Unit,
    onBatesPaddingChanged: (Int) -> Unit,
    onApply: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.LibraryAddCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("Batch Watermark", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Apply legal-grade watermark text and incremental Bates numbering across multiple PDFs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickInputs)
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
                }
            }

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
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Text("STAMP SETTINGS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                CustomSwitchRow("Enable Watermark", state.enableWatermark, onEnableWatermarkChanged)
                if (state.enableWatermark) {
                    OutlinedTextField(
                        value = state.watermarkText,
                        onValueChange = onWatermarkTextChanged,
                        label = { Text("Watermark Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CustomSwitchRow("Enable Bates Numbering", state.enableBates, onEnableBatesChanged)
                if (state.enableBates) {
                    OutlinedTextField(
                        value = state.batesPrefix,
                        onValueChange = onBatesPrefixChanged,
                        label = { Text("Bates Prefix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.batesStart,
                            onValueChange = onBatesStartChanged,
                            label = { Text("Bates Start") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Padding: ${state.batesPadding}", style = MaterialTheme.typography.labelSmall)
                            Row {
                                Button(onClick = { onBatesPaddingChanged((state.batesPadding - 1).coerceAtLeast(1)) }) { Text("-") }
                                Spacer(Modifier.width(4.dp))
                                Button(onClick = { onBatesPaddingChanged((state.batesPadding + 1).coerceAtMost(10)) }) { Text("+") }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.outputBaseName,
                    onValueChange = onOutputBaseNameChanged,
                    label = { Text("Output Base Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.inputLabels.isNotEmpty()) {
            Text(
                text = "SELECTED FILES (${state.inputLabels.size})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.inputLabels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rotation by animateFloatAsState(
                targetValue = if (state.isProcessing) 360f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )

            Button(
                onClick = onApply,
                enabled = !state.isProcessing && state.inputLabels.isNotEmpty(),
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
                    Text("Apply Batch Stamps", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }

        if (state.statusMessage != null || state.errorMessage != null || state.lastOutputPaths.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp) }
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onClearError, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Dismiss Error")
                    }
                }
                if (state.lastOutputPaths.isNotEmpty()) {
                    Text("Saved files:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    state.lastOutputPaths.forEach { path -> Text(path, style = MaterialTheme.typography.bodySmall) }
                }
                state.lastOutputSizeBytes?.let { Text("Total output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
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
