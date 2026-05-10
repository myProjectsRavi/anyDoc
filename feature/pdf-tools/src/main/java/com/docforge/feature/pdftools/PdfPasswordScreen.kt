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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PdfPasswordRoute(
    viewModel: PdfPasswordViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val label = readLabel(context, uri)
            viewModel.onInputSelected(uri, label)
        }
    }

    PdfPasswordScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onModeChanged = viewModel::onModeChanged,
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onUserPasswordChanged = viewModel::onUserPasswordChanged,
        onOwnerPasswordChanged = viewModel::onOwnerPasswordChanged,
        onUnlockPasswordChanged = viewModel::onUnlockPasswordChanged,
        onApply = viewModel::applyPasswordAction,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfPasswordScreen(
    state: PdfPasswordUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onModeChanged: (PdfPasswordMode) -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onUserPasswordChanged: (String) -> Unit,
    onOwnerPasswordChanged: (String) -> Unit,
    onUnlockPasswordChanged: (String) -> Unit,
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
        // Title & Subtitle
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("PDF Password", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Add or remove PDF password protection fully offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

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
                Text("Select PDF Document", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (state.inputLabel != null) {
                    Text(state.inputLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    state.isEncrypted?.let { encrypted ->
                        Text(
                            text = if (encrypted) "Encrypted" else "Unprotected",
                            color = if (encrypted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                    }
                } else {
                    Text("Tap to browse files", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }

        // Settings Bento
        if (state.selectedUri != null) {
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
                    Text("SECURITY SETTINGS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                OutlinedTextField(
                    value = state.outputName,
                    onValueChange = onOutputNameChanged,
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfPasswordMode.entries.forEach { mode ->
                        val selected = state.mode == mode
                        Button(
                            onClick = { onModeChanged(mode) },
                            enabled = !state.isProcessing,
                            colors = if (selected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) 
                                     else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (mode == PdfPasswordMode.PROTECT) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.name)
                        }
                    }
                }

                if (state.mode == PdfPasswordMode.PROTECT) {
                    OutlinedTextField(
                        value = state.userPasswordInput,
                        onValueChange = onUserPasswordChanged,
                        label = { Text("User Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.ownerPasswordInput,
                        onValueChange = onOwnerPasswordChanged,
                        label = { Text("Owner Password (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = state.unlockPasswordInput,
                        onValueChange = onUnlockPasswordChanged,
                        label = { Text("Current PDF Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Process Button
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rotation by animateFloatAsState(targetValue = if (state.isProcessing) 360f else 0f, animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing))

            Button(
                onClick = onApply,
                enabled = !state.isProcessing && state.selectedUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(64.dp).shadow(8.dp, CircleShape)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing...", fontWeight = FontWeight.Black, fontSize = 18.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotation))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (state.mode == PdfPasswordMode.PROTECT) "Protect PDF" else "Unlock PDF", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }

        // Status & Progress
        if (state.isProcessing || state.statusMessage != null || state.errorMessage != null || state.lastOutputPath != null) {
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
                
                state.lastOutputPath?.let { 
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saved: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.lastOutputSizeBytes?.let { size -> Text("Output size: $size bytes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}