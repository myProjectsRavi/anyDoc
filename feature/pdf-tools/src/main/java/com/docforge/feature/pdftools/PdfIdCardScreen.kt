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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.pdf.PdfPageSize

@Composable
fun PdfIdCardRoute(
    viewModel: PdfIdCardViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current

    val frontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onFrontSelected(uri, readLabel(context, uri))
    }

    val backPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onBackSelected(uri, readLabel(context, uri))
    }

    PdfIdCardScreen(
        state = state,
        paddingValues = paddingValues,
        onPickFront = { frontPicker.launch("image/*") },
        onPickBack = { backPicker.launch("image/*") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onPageSizeChanged = viewModel::onPageSizeChanged,
        onExport = viewModel::exportIdSheet,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfIdCardScreen(
    state: PdfIdCardUiState,
    paddingValues: PaddingValues,
    onPickFront: () -> Unit,
    onPickBack: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onPageSizeChanged: (PdfPageSize) -> Unit,
    onExport: () -> Unit,
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
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("ID Card / Passport", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Scan/select front + back and auto-compose both on one printable A4/Letter sheet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Drop Zones
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Front Drop Zone
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickFront)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Text("Front Side", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (state.frontLabel != null) {
                        Text(state.frontLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("Tap to select", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            // Back Drop Zone
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickBack)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Text("Back Side", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (state.backLabel != null) {
                        Text(state.backLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("Tap to select", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }

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
                Text("DOCUMENT SETTINGS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            OutlinedTextField(
                value = state.outputName,
                onValueChange = onOutputNameChanged,
                label = { Text("Output File Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Page Size", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PdfPageSize.A4, PdfPageSize.LETTER).forEach { size ->
                    val selected = state.pageSize == size
                    Button(
                        onClick = { onPageSizeChanged(size) },
                        enabled = !state.isProcessing,
                        colors = if (selected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) 
                                 else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(size.name)
                    }
                }
            }
        }

        // Process Button Section
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rotation by animateFloatAsState(
                targetValue = if (state.isProcessing) 360f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing)
            )

            Button(
                onClick = onExport,
                enabled = !state.isProcessing && state.frontUri != null && state.backUri != null,
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
                    Text("Export ID Sheet", fontWeight = FontWeight.Black, fontSize = 18.sp)
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
                state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
                state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
            }
        }
        
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}
