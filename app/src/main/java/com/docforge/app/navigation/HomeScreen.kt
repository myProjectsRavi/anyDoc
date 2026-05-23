package com.docforge.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TinyWow-inspired HomeScreen — ultra-clean with two main category cards (PDF & Image),
 * plus quick access to scanner, vault, audio/video tools, and recent history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    paddingValues: PaddingValues,
    onOpenScanner: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenImageFormatConverter: () -> Unit,
    onOpenAudioFormatConverter: () -> Unit,
    onOpenDocumentToPdf: () -> Unit,
    onOpenTextToPdf: () -> Unit,
    onOpenVideoToAudio: () -> Unit,
    onOpenBatchQueue: () -> Unit,
    onOpenPdfMerge: () -> Unit,
    onOpenPdfSplit: () -> Unit,
    onOpenPdfSign: () -> Unit,
    onOpenPdfAnnotate: () -> Unit,
    onOpenPdfPassword: () -> Unit,
    onOpenPdfCompress: () -> Unit,
    onOpenPdfText: () -> Unit,
    onOpenPdfToImages: () -> Unit,
    onOpenPdfBatchStamp: () -> Unit,
    onOpenPdfOcr: () -> Unit,
    onOpenPdfForm: () -> Unit,
    onOpenIdCard: () -> Unit,
    onOpenPdfRedact: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVault: () -> Unit = {},
    onOpenPdfHub: () -> Unit = {},
    onOpenImageHub: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val tools = remember(
        onOpenScanner, onOpenConverter, onOpenImageFormatConverter, onOpenAudioFormatConverter,
        onOpenDocumentToPdf, onOpenTextToPdf, onOpenVideoToAudio, onOpenBatchQueue, onOpenPdfMerge,
        onOpenPdfSplit, onOpenPdfSign, onOpenPdfAnnotate, onOpenPdfPassword, onOpenPdfCompress,
        onOpenPdfText, onOpenPdfToImages, onOpenPdfBatchStamp, onOpenPdfOcr, onOpenPdfForm,
        onOpenIdCard, onOpenPdfRedact, onOpenHistory, onOpenSettings
    ) {
        buildHomeTools(
            onOpenScanner, onOpenConverter, onOpenImageFormatConverter, onOpenAudioFormatConverter,
            onOpenDocumentToPdf, onOpenTextToPdf, onOpenVideoToAudio, onOpenBatchQueue, onOpenPdfMerge,
            onOpenPdfSplit, onOpenPdfSign, onOpenPdfAnnotate, onOpenPdfPassword, onOpenPdfCompress,
            onOpenPdfText, onOpenPdfToImages, onOpenPdfBatchStamp, onOpenPdfOcr, onOpenPdfForm,
            onOpenIdCard, onOpenPdfRedact, onOpenHistory, onOpenSettings
        )
    }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredTools = remember(normalizedQuery, tools) {
        if (normalizedQuery.isBlank()) emptyList()
        else tools.filter { tool ->
            tool.title.lowercase().contains(normalizedQuery) ||
                tool.subtitle.lowercase().contains(normalizedQuery) ||
                tool.keywords.lowercase().contains(normalizedQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    "AnyDoc",
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tagline
            Text(
                "Your private document toolkit.\nNo internet. No servers. Just you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search all tools...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            if (normalizedQuery.isNotBlank()) {
                // Search Results
                Text("${filteredTools.size} results", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                filteredTools.forEach { tool ->
                    SearchResultCard(title = tool.title, subtitle = tool.subtitle, onClick = tool.onClick)
                }
            } else {
                // ═══════════════ MAIN CATEGORY CARDS (TinyWow Style) ═══════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(
                        modifier = Modifier.weight(1f),
                        title = "PDF",
                        subtitle = "19 tools",
                        icon = Icons.Default.PictureAsPdf,
                        gradientColors = listOf(Color(0xFFE11D48), Color(0xFFF43F5E)),
                        onClick = onOpenPdfHub
                    )
                    CategoryCard(
                        modifier = Modifier.weight(1f),
                        title = "Image",
                        subtitle = "7 tools",
                        icon = Icons.Default.Image,
                        gradientColors = listOf(Color(0xFF6366F1), Color(0xFF818CF8)),
                        onClick = onOpenImageHub
                    )
                }

                // ═══════════════ QUICK ACCESS STRIP ═══════════════
                Text("Quick Access", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessChip("Scanner", Icons.Default.CameraAlt, Color(0xFF10B981), onOpenScanner)
                    QuickAccessChip("Vault", Icons.Default.Shield, Color(0xFF7C3AED), onOpenVault)
                    QuickAccessChip("Audio", Icons.Default.Audiotrack, Color(0xFFF59E0B), onOpenAudioFormatConverter)
                    QuickAccessChip("Video → Audio", Icons.Default.Movie, Color(0xFF0EA5E9), onOpenVideoToAudio)
                    QuickAccessChip("Batch Queue", Icons.Default.History, Color(0xFF6366F1), onOpenBatchQueue)
                }

                // ═══════════════ POPULAR PDF TOOLS ═══════════════
                SectionHeader("Popular PDF Tools", "See all", onOpenPdfHub)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniToolChip("Merge", Color(0xFF6366F1), onOpenPdfMerge)
                    MiniToolChip("Split", Color(0xFF8B5CF6), onOpenPdfSplit)
                    MiniToolChip("Compress", Color(0xFF06B6D4), onOpenPdfCompress)
                    MiniToolChip("Sign", Color(0xFFEC4899), onOpenPdfSign)
                    MiniToolChip("OCR", Color(0xFF16A34A), onOpenPdfOcr)
                    MiniToolChip("Redact", Color(0xFFDC2626), onOpenPdfRedact)
                    MiniToolChip("Protect", Color(0xFFEF4444), onOpenPdfPassword)
                }

                // ═══════════════ POPULAR IMAGE TOOLS ═══════════════
                SectionHeader("Popular Image Tools", "See all", onOpenImageHub)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniToolChip("Convert", Color(0xFF6366F1), onOpenImageFormatConverter)
                    MiniToolChip("Resize", Color(0xFF8B5CF6), onOpenImageFormatConverter)
                    MiniToolChip("Compress", Color(0xFF06B6D4), onOpenImageFormatConverter)
                    MiniToolChip("Scan", Color(0xFF10B981), onOpenScanner)
                }

                // ═══════════════ RECENT ACTIVITY ═══════════════
                if (state.recentItems.isNotEmpty()) {
                    SectionHeader("Recent", "View All", onOpenHistory)
                    state.recentItems.take(5).forEach { item ->
                        SearchResultCard(title = item.title, subtitle = item.subtitle, onClick = onOpenHistory)
                    }
                }

                // Privacy Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Column {
                            Text("100% Offline & Private", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Your files never leave your device", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                    Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun QuickAccessChip(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = tint)
        }
    }
}

@Composable
private fun MiniToolChip(title: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.clickable(onClick = onAction),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(actionText, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SearchResultCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun buildHomeTools(
    onOpenScanner: () -> Unit, onOpenConverter: () -> Unit, onOpenImageFormatConverter: () -> Unit,
    onOpenAudioFormatConverter: () -> Unit, onOpenDocumentToPdf: () -> Unit, onOpenTextToPdf: () -> Unit,
    onOpenVideoToAudio: () -> Unit, onOpenBatchQueue: () -> Unit, onOpenPdfMerge: () -> Unit,
    onOpenPdfSplit: () -> Unit, onOpenPdfSign: () -> Unit, onOpenPdfAnnotate: () -> Unit,
    onOpenPdfPassword: () -> Unit, onOpenPdfCompress: () -> Unit, onOpenPdfText: () -> Unit,
    onOpenPdfToImages: () -> Unit, onOpenPdfBatchStamp: () -> Unit, onOpenPdfOcr: () -> Unit,
    onOpenPdfForm: () -> Unit, onOpenIdCard: () -> Unit,
    onOpenPdfRedact: () -> Unit, onOpenHistory: () -> Unit, onOpenSettings: () -> Unit
): List<HomeTool> {
    return listOf(
        HomeTool(HomeToolId.IMAGES_TO_PDF, "Images to PDF", "Convert multiple photos into one PDF fully offline", "jpg png heic photos pdf convert", onOpenConverter),
        HomeTool(HomeToolId.IMAGE_FORMAT, "Image Format Convert", "Convert HEIC/WebP/BMP/TIFF/JPG/PNG images", "heic webp bmp tiff jpg png resize crop", onOpenImageFormatConverter),
        HomeTool(HomeToolId.AUDIO_FORMAT, "Audio Format Convert", "Convert audio between M4A/WAV/MP3/FLAC offline", "audio m4a wav mp3 flac", onOpenAudioFormatConverter),
        HomeTool(HomeToolId.DOC_TO_PDF, "Document to PDF", "Convert DOCX/RTF/CSV/TXT files to PDF offline", "doc docx rtf csv txt pdf word", onOpenDocumentToPdf),
        HomeTool(HomeToolId.TEXT_TO_PDF, "Text to PDF", "Type or paste text and export a formatted PDF", "text notes markdown write pdf", onOpenTextToPdf),
        HomeTool(HomeToolId.VIDEO_TO_AUDIO, "Video to Audio", "Extract M4A or MP3 audio from local videos", "video mp4 mov m4a mp3 extract", onOpenVideoToAudio),
        HomeTool(HomeToolId.BATCH_QUEUE, "Batch Queue", "Queue mixed tasks and run sequentially offline", "batch queue automation sequential", onOpenBatchQueue),
        HomeTool(HomeToolId.HISTORY, "Recent Files", "Open your last 20 conversions", "history recent", onOpenHistory),
        HomeTool(HomeToolId.SETTINGS, "Settings", "Defaults for page size, quality, folders, onboarding", "settings defaults quality page size output folders", onOpenSettings),
        HomeTool(HomeToolId.SCANNER, "Document Scanner", "Capture scans, export PDF/JPG/PNG, optional ZIP bundle", "scan camera pdf jpg png zip bundle document", onOpenScanner),
        HomeTool(HomeToolId.PDF_MERGE, "Merge PDF", "Combine multiple PDFs into one offline", "pdf merge combine", onOpenPdfMerge),
        HomeTool(HomeToolId.PDF_SPLIT, "Split PDF", "Extract pages or split by range", "pdf split extract pages", onOpenPdfSplit),
        HomeTool(HomeToolId.PDF_SIGN, "Sign PDF", "Draw or reuse signatures", "pdf sign signature", onOpenPdfSign),
        HomeTool(HomeToolId.PDF_ANNOTATE, "Annotate PDF", "Highlight, draw, add notes", "pdf annotate highlight note", onOpenPdfAnnotate),
        HomeTool(HomeToolId.PDF_PASSWORD, "Protect / Unlock PDF", "Add or remove password", "pdf password protect unlock encrypt", onOpenPdfPassword),
        HomeTool(HomeToolId.PDF_COMPRESS, "Compress PDF", "Reduce PDF file size", "pdf compress reduce size", onOpenPdfCompress),
        HomeTool(HomeToolId.PDF_TEXT, "PDF to Text", "Extract text from PDF", "pdf text txt extract", onOpenPdfText),
        HomeTool(HomeToolId.PDF_TO_IMAGES, "PDF to Images", "Export pages as JPG/PNG/WebP", "pdf images jpg png webp", onOpenPdfToImages),
        HomeTool(HomeToolId.PDF_BATCH_STAMP, "Watermark & Bates", "Add watermarks or legal numbering", "pdf watermark bates stamp", onOpenPdfBatchStamp),
        HomeTool(HomeToolId.PDF_OCR, "OCR (Text Recognition)", "Make scanned PDFs searchable", "ocr text recognition searchable scan", onOpenPdfOcr),
        HomeTool(HomeToolId.PDF_FORM, "Fill PDF Forms", "Fill form fields and build new forms", "pdf form acroform fill", onOpenPdfForm),
        HomeTool(HomeToolId.ID_CARD, "ID Card to PDF", "Front+back on one printable page", "id card passport scan", onOpenIdCard),
        HomeTool(HomeToolId.PDF_REDACT, "Redact PDF", "Permanently remove sensitive text", "pdf redact remove text secure", onOpenPdfRedact)
    )
}
