package com.docforge.app.navigation

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HomeTool(
    val id: HomeToolId,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val iconBackground: Color = Color.Transparent,
    val onIconColor: Color = Color.White
)

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
    onOpenPdfTranslate: () -> Unit,
    onOpenPdfRedact: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVault: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val tools = remember(
        onOpenScanner, onOpenConverter, onOpenImageFormatConverter, onOpenAudioFormatConverter,
        onOpenDocumentToPdf, onOpenTextToPdf, onOpenVideoToAudio, onOpenBatchQueue, onOpenPdfMerge,
        onOpenPdfSplit, onOpenPdfSign, onOpenPdfAnnotate, onOpenPdfPassword, onOpenPdfCompress,
        onOpenPdfText, onOpenPdfToImages, onOpenPdfBatchStamp, onOpenPdfOcr, onOpenPdfForm,
        onOpenIdCard, onOpenPdfTranslate, onOpenPdfRedact, onOpenHistory, onOpenSettings
    ) {
        buildHomeTools(
            onOpenScanner, onOpenConverter, onOpenImageFormatConverter, onOpenAudioFormatConverter,
            onOpenDocumentToPdf, onOpenTextToPdf, onOpenVideoToAudio, onOpenBatchQueue, onOpenPdfMerge,
            onOpenPdfSplit, onOpenPdfSign, onOpenPdfAnnotate, onOpenPdfPassword, onOpenPdfCompress,
            onOpenPdfText, onOpenPdfToImages, onOpenPdfBatchStamp, onOpenPdfOcr, onOpenPdfForm,
            onOpenIdCard, onOpenPdfTranslate, onOpenPdfRedact, onOpenHistory, onOpenSettings
        )
    }

    val quickActionTools = remember(state.quickActionToolIds, tools) {
        val recent = state.quickActionToolIds.mapNotNull { id -> tools.firstOrNull { tool -> tool.id == id } }
        if (recent.isNotEmpty()) recent else tools.take(4)
    }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredTools = remember(normalizedQuery, tools) {
        if (normalizedQuery.isBlank()) {
            tools
        } else {
            tools.filter { tool ->
                tool.title.lowercase().contains(normalizedQuery) ||
                    tool.subtitle.lowercase().contains(normalizedQuery) ||
                    tool.keywords.lowercase().contains(normalizedQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AnyDoc",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenScanner,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding() + 16.dp)
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Now", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Ready to transform?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Search for any document tool or file",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Try 'Merge Invoices'...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(2.dp, CircleShape),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (normalizedQuery.isBlank()) {
                // Recents
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Recents", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Text("View All", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onOpenHistory() })
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dummy recent items
                        RecentCard("Annual_Report.pdf", "Modified 2h ago", MaterialTheme.colorScheme.secondaryContainer)
                        RecentCard("Contract_Signed.ocr", "Modified 5h ago", MaterialTheme.colorScheme.primaryContainer)
                        RecentCard("Travel_Booking.pdf", "Yesterday", MaterialTheme.colorScheme.tertiaryContainer)
                    }
                }

                // Super App Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Powerful Tools", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    
                    val mainTools = listOf(
                        HomeTool(HomeToolId.PDF_MERGE, "Merge PDF", "", "", onOpenPdfMerge, Icons.Default.CallMerge, MaterialTheme.colorScheme.primary),
                        HomeTool(HomeToolId.PDF_REDACT, "Redact", "", "", onOpenPdfRedact, Icons.Outlined.Edit, MaterialTheme.colorScheme.secondary),
                        HomeTool(HomeToolId.PDF_COMPRESS, "Compress", "", "", onOpenPdfCompress, Icons.Default.Compress, MaterialTheme.colorScheme.tertiary),
                        HomeTool(HomeToolId.PDF_OCR, "OCR", "", "", onOpenPdfOcr, Icons.Default.Spellcheck, MaterialTheme.colorScheme.primary),
                        HomeTool(HomeToolId.PDF_TRANSLATE, "Translate", "", "", onOpenPdfTranslate, Icons.Default.Translate, MaterialTheme.colorScheme.secondary),
                        HomeTool(HomeToolId.DOC_TO_PDF, "Converter", "", "", onOpenDocumentToPdf, Icons.Default.SyncAlt, MaterialTheme.colorScheme.tertiary)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(240.dp) // Adjust height as needed
                    ) {
                        items(mainTools) { tool ->
                            ToolGridItem(tool)
                        }
                    }
                }

                // Bento Highlights
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BentoCard(
                        title = "Doc Vault",
                        subtitle = "Secure your sensitive documents with military-grade encryption.",
                        buttonText = "Go to Vault",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        borderColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                        titleColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        buttonColor = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenVault
                        )
                    BentoCard(
                        title = "Smart Cleanup",
                        subtitle = "AI-powered duplicate removal and storage optimization.",
                        buttonText = "Clean Now",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        borderColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                        titleColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        buttonColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { /* TODO Cleanup */ }
                    )
                }
            } else {
                Text("Search Results (${filteredTools.size})", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredTools.forEach { tool ->
                        ToolCard(title = tool.title, subtitle = tool.subtitle, onClick = tool.onClick)
                    }
                }
            }
            Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
        }
    }
}

@Composable
private fun RecentCard(title: String, subtitle: String, imageColor: Color) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(imageColor)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                )
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ToolGridItem(tool: HomeTool) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = tool.onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tool.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                if (tool.icon != null) {
                    Icon(tool.icon, contentDescription = null, tint = tool.onIconColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(tool.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun BentoCard(
    title: String,
    subtitle: String,
    buttonText: String,
    containerColor: Color,
    borderColor: Color,
    titleColor: Color,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp, color = titleColor)
                Text(subtitle, color = titleColor.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color.White),
                shape = CircleShape
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
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
    onOpenPdfForm: () -> Unit, onOpenIdCard: () -> Unit, onOpenPdfTranslate: () -> Unit,
    onOpenPdfRedact: () -> Unit, onOpenHistory: () -> Unit, onOpenSettings: () -> Unit
): List<HomeTool> {
    return listOf(
        HomeTool(HomeToolId.IMAGES_TO_PDF, "Images to PDF", "Convert multiple photos into one PDF fully offline", "jpg png heic photos pdf convert", onOpenConverter),
        HomeTool(HomeToolId.IMAGE_FORMAT, "Image Format Convert", "Convert HEIC/WebP/BMP/TIFF/JPG/PNG images", "heic webp bmp tiff jpg png", onOpenImageFormatConverter),
        HomeTool(HomeToolId.AUDIO_FORMAT, "Audio Format Convert", "Convert audio between M4A/WAV/MP3/FLAC offline", "audio m4a wav mp3 flac", onOpenAudioFormatConverter),
        HomeTool(HomeToolId.DOC_TO_PDF, "Document to PDF", "Convert DOCX/RTF/CSV/TXT files to PDF offline", "doc docx rtf csv txt pdf", onOpenDocumentToPdf),
        HomeTool(HomeToolId.TEXT_TO_PDF, "Text to PDF", "Type or paste text and export a formatted PDF", "text notes markdown write pdf", onOpenTextToPdf),
        HomeTool(HomeToolId.VIDEO_TO_AUDIO, "Video to Audio", "Extract M4A or MP3 audio from local videos", "video mp4 mov m4a mp3 extract", onOpenVideoToAudio),
        HomeTool(HomeToolId.BATCH_QUEUE, "Batch Queue", "Queue mixed tasks and run sequentially offline", "batch queue automation sequential", onOpenBatchQueue),
        HomeTool(HomeToolId.HISTORY, "Recent Files", "Open your last 20 conversions", "history recent", onOpenHistory),
        HomeTool(HomeToolId.SETTINGS, "Settings", "Defaults for page size, quality, folders, onboarding", "settings defaults quality page size output folders", onOpenSettings),
        HomeTool(HomeToolId.SCANNER, "Document Scanner", "Capture scans, export PDF/JPG/PNG, optional ZIP bundle", "scan camera pdf jpg png zip bundle", onOpenScanner),
        HomeTool(HomeToolId.PDF_MERGE, "PDF Merge", "Merge PDFs + images into one PDF offline", "pdf merge combine images scan", onOpenPdfMerge),
        HomeTool(HomeToolId.PDF_SPLIT, "PDF Split/Extract", "Split/extract with virtualized thumbnails + prefetch", "pdf split extract pages thumbnails reorder delete rotate quality prefetch", onOpenPdfSplit),
        HomeTool(HomeToolId.PDF_SIGN, "PDF Sign", "Draw/reuse signature with templates and multi-placement", "pdf sign signature drag preview templates multiple placements all pages", onOpenPdfSign),
        HomeTool(HomeToolId.PDF_ANNOTATE, "PDF Annotate", "Highlight, text, sticky-note, and freehand drawing", "pdf annotate highlight note freehand draw", onOpenPdfAnnotate),
        HomeTool(HomeToolId.PDF_PASSWORD, "PDF Password", "Add or remove password protection offline", "pdf password protect unlock", onOpenPdfPassword),
        HomeTool(HomeToolId.PDF_COMPRESS, "PDF Compress", "Compress PDFs with quality presets", "pdf compress reduce size", onOpenPdfCompress),
        HomeTool(HomeToolId.PDF_TEXT, "PDF to TXT", "Extract text into a local .txt file", "pdf text txt extract", onOpenPdfText),
        HomeTool(HomeToolId.PDF_TO_IMAGES, "PDF to Images", "Export pages to JPG/PNG/WebP with optional ZIP bundle", "pdf images jpg png webp zip bundle", onOpenPdfToImages),
        HomeTool(HomeToolId.PDF_BATCH_STAMP, "Batch Watermark + Bates", "Apply legal watermark text and Bates numbering across PDFs", "pdf batch watermark bates legal stamp numbering", onOpenPdfBatchStamp),
        HomeTool(HomeToolId.PDF_OCR, "Offline OCR", "Extract text from scans and generate searchable PDFs", "ocr text recognition searchable pdf scan", onOpenPdfOcr),
        HomeTool(HomeToolId.PDF_FORM, "AcroForm Fill + Builder", "Fill existing form fields and add new text fields", "pdf form acroform fill builder fields", onOpenPdfForm),
        HomeTool(HomeToolId.ID_CARD, "ID Card / Passport Mode", "Front+back auto layout to one printable PDF page", "id card passport front back scan pdf", onOpenIdCard),
        HomeTool(HomeToolId.PDF_TRANSLATE, "Auto-Translate PDF", "On-device translation with line-level layout overlays", "translate pdf language offline preserve layout", onOpenPdfTranslate),
        HomeTool(HomeToolId.PDF_REDACT, "True PDF Redaction", "Content-level text removal with irreversible verification", "pdf redact redaction irreversible remove text secure", onOpenPdfRedact)
    )
}

@Composable
private fun ToolCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
