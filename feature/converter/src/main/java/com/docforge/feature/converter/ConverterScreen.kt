package com.docforge.feature.converter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.pdf.PdfPageSize

@Composable
fun ConverterRoute(
    viewModel: ConverterViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onOpenImageFormat: () -> Unit = {},
    onOpenAudioFormat: () -> Unit = {},
    onOpenDocumentToPdf: () -> Unit = {},
    onOpenTextToPdf: () -> Unit = {},
    onOpenVideoToAudio: () -> Unit = {},
    onOpenPdfToImages: () -> Unit = {},
    onOpenPdfToText: () -> Unit = {}
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
        onClearError = viewModel::clearError,
        onOpenImageFormat = onOpenImageFormat,
        onOpenAudioFormat = onOpenAudioFormat,
        onOpenDocumentToPdf = onOpenDocumentToPdf,
        onOpenTextToPdf = onOpenTextToPdf,
        onOpenVideoToAudio = onOpenVideoToAudio,
        onOpenPdfToImages = onOpenPdfToImages,
        onOpenPdfToText = onOpenPdfToText
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
    onClearError: () -> Unit,
    onOpenImageFormat: () -> Unit = {},
    onOpenAudioFormat: () -> Unit = {},
    onOpenDocumentToPdf: () -> Unit = {},
    onOpenTextToPdf: () -> Unit = {},
    onOpenVideoToAudio: () -> Unit = {},
    onOpenPdfToImages: () -> Unit = {},
    onOpenPdfToText: () -> Unit = {}
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
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("Images to PDF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Offline conversion. Your files never leave your phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Conversion hub: shortcuts to all available converters
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MORE CONVERSIONS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConverterTile("Document → PDF", "DOCX • RTF • CSV • TXT", Icons.Default.Description, onOpenDocumentToPdf)
                ConverterTile("Text → PDF", "Type or paste text", Icons.Default.TextSnippet, onOpenTextToPdf)
                ConverterTile("PDF → Images", "JPG / PNG / WebP", Icons.Default.PictureAsPdf, onOpenPdfToImages)
                ConverterTile("PDF → TXT", "Extract text", Icons.Default.TextSnippet, onOpenPdfToText)
                ConverterTile("Image Format", "HEIC • WebP • JPG • PNG", Icons.Default.Image, onOpenImageFormat)
                ConverterTile("Audio Format", "M4A • WAV • MP3 • FLAC", Icons.Default.Audiotrack, onOpenAudioFormat)
                ConverterTile("Video → Audio", "Extract M4A / MP3", Icons.Default.Movie, onOpenVideoToAudio)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPickImages)
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
                    Text("Upload Images", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Tap to browse files", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }

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
                    Text("OUTPUT SETTINGS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                OutlinedTextField(
                    value = state.outputName,
                    onValueChange = onOutputNameChanged,
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Page Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfPageSize.entries.forEach { size ->
                        FilterChip(
                            selected = size == state.pageSize,
                            onClick = { onPageSizeChanged(size) },
                            label = { Text(size.name) },
                            enabled = !state.isConverting
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "SELECTED IMAGES (${state.selectedUris.size})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            if (state.selectedUris.isNotEmpty()) {
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
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${state.selectedUris.size} Images Selected", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Ready to convert", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text("No images selected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rotation by animateFloatAsState(
                targetValue = if (state.isConverting) 360f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing)
            )

            Button(
                onClick = onConvert,
                enabled = !state.isConverting && state.selectedUris.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(64.dp).shadow(8.dp, CircleShape)
            ) {
                if (state.isConverting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Converting...", fontWeight = FontWeight.Black, fontSize = 18.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = rotation))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Convert to PDF", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }

        if (state.isConverting || state.statusMessage != null || state.errorMessage != null || state.lastOutputPath != null) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isConverting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                }
                state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp) }
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onClearError, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Dismiss Error") }
                }
                state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
                state.lastOutputSizeBytes?.let { Text("Size: $it bytes", style = MaterialTheme.typography.bodySmall) }
            }
        }
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
    }
}

@Composable
private fun ConverterTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}