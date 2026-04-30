package com.docforge.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HomeTool(
    val id: HomeToolId,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val onClick: () -> Unit
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
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
    onOpenSettings: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val tools = remember(
        onOpenScanner,
        onOpenConverter,
        onOpenImageFormatConverter,
        onOpenAudioFormatConverter,
        onOpenDocumentToPdf,
        onOpenTextToPdf,
        onOpenVideoToAudio,
        onOpenBatchQueue,
        onOpenPdfMerge,
        onOpenPdfSplit,
        onOpenPdfSign,
        onOpenPdfAnnotate,
        onOpenPdfPassword,
        onOpenPdfCompress,
        onOpenPdfText,
        onOpenPdfToImages,
        onOpenPdfBatchStamp,
        onOpenPdfOcr,
        onOpenPdfForm,
        onOpenIdCard,
        onOpenPdfTranslate,
        onOpenPdfRedact,
        onOpenHistory,
        onOpenSettings
    ) {
        buildHomeTools(
            onOpenScanner = onOpenScanner,
            onOpenConverter = onOpenConverter,
            onOpenImageFormatConverter = onOpenImageFormatConverter,
            onOpenAudioFormatConverter = onOpenAudioFormatConverter,
            onOpenDocumentToPdf = onOpenDocumentToPdf,
            onOpenTextToPdf = onOpenTextToPdf,
            onOpenVideoToAudio = onOpenVideoToAudio,
            onOpenBatchQueue = onOpenBatchQueue,
            onOpenPdfMerge = onOpenPdfMerge,
            onOpenPdfSplit = onOpenPdfSplit,
            onOpenPdfSign = onOpenPdfSign,
            onOpenPdfAnnotate = onOpenPdfAnnotate,
            onOpenPdfPassword = onOpenPdfPassword,
            onOpenPdfCompress = onOpenPdfCompress,
            onOpenPdfText = onOpenPdfText,
            onOpenPdfToImages = onOpenPdfToImages,
            onOpenPdfBatchStamp = onOpenPdfBatchStamp,
            onOpenPdfOcr = onOpenPdfOcr,
            onOpenPdfForm = onOpenPdfForm,
            onOpenIdCard = onOpenIdCard,
            onOpenPdfTranslate = onOpenPdfTranslate,
            onOpenPdfRedact = onOpenPdfRedact,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DocForge",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("What do you want to do?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Quick Actions", fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickActionTools.forEach { tool ->
                Button(onClick = tool.onClick) {
                    Text(tool.title)
                }
            }
        }

        Text(
            if (normalizedQuery.isBlank()) "All Tools" else "Search Results (${filteredTools.size})",
            fontWeight = FontWeight.SemiBold
        )

        filteredTools.forEach { tool ->
            ToolCard(
                title = tool.title,
                subtitle = tool.subtitle,
                onClick = tool.onClick
            )
        }
    }
}

private fun buildHomeTools(
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
    onOpenSettings: () -> Unit
): List<HomeTool> {
    return listOf(
        HomeTool(
            id = HomeToolId.IMAGES_TO_PDF,
            title = "Images to PDF",
            subtitle = "Convert multiple photos into one PDF fully offline",
            keywords = "jpg png heic photos pdf convert",
            onClick = onOpenConverter
        ),
        HomeTool(
            id = HomeToolId.IMAGE_FORMAT,
            title = "Image Format Convert",
            subtitle = "Convert HEIC/WebP/BMP/TIFF/JPG/PNG images",
            keywords = "heic webp bmp tiff jpg png",
            onClick = onOpenImageFormatConverter
        ),
        HomeTool(
            id = HomeToolId.AUDIO_FORMAT,
            title = "Audio Format Convert",
            subtitle = "Convert audio between M4A/WAV/MP3/FLAC offline",
            keywords = "audio m4a wav mp3 flac",
            onClick = onOpenAudioFormatConverter
        ),
        HomeTool(
            id = HomeToolId.DOC_TO_PDF,
            title = "Document to PDF",
            subtitle = "Convert DOCX/RTF/CSV/TXT files to PDF offline",
            keywords = "doc docx rtf csv txt pdf",
            onClick = onOpenDocumentToPdf
        ),
        HomeTool(
            id = HomeToolId.TEXT_TO_PDF,
            title = "Text to PDF",
            subtitle = "Type or paste text and export a formatted PDF",
            keywords = "text notes markdown write pdf",
            onClick = onOpenTextToPdf
        ),
        HomeTool(
            id = HomeToolId.VIDEO_TO_AUDIO,
            title = "Video to Audio",
            subtitle = "Extract M4A or MP3 audio from local videos",
            keywords = "video mp4 mov m4a mp3 extract",
            onClick = onOpenVideoToAudio
        ),
        HomeTool(
            id = HomeToolId.BATCH_QUEUE,
            title = "Batch Queue",
            subtitle = "Queue mixed tasks and run sequentially offline",
            keywords = "batch queue automation sequential",
            onClick = onOpenBatchQueue
        ),
        HomeTool(
            id = HomeToolId.HISTORY,
            title = "Recent Files",
            subtitle = "Open your last 20 conversions",
            keywords = "history recent",
            onClick = onOpenHistory
        ),
        HomeTool(
            id = HomeToolId.SETTINGS,
            title = "Settings",
            subtitle = "Defaults for page size, quality, folders, onboarding",
            keywords = "settings defaults quality page size output folders",
            onClick = onOpenSettings
        ),
        HomeTool(
            id = HomeToolId.SCANNER,
            title = "Document Scanner",
            subtitle = "Capture scans, export PDF/JPG/PNG, optional ZIP bundle",
            keywords = "scan camera pdf jpg png zip bundle",
            onClick = onOpenScanner
        ),
        HomeTool(
            id = HomeToolId.PDF_MERGE,
            title = "PDF Merge",
            subtitle = "Merge PDFs + images into one PDF offline",
            keywords = "pdf merge combine images scan",
            onClick = onOpenPdfMerge
        ),
        HomeTool(
            id = HomeToolId.PDF_SPLIT,
            title = "PDF Split/Extract",
            subtitle = "Split/extract with virtualized thumbnails + prefetch",
            keywords = "pdf split extract pages thumbnails reorder delete rotate quality prefetch",
            onClick = onOpenPdfSplit
        ),
        HomeTool(
            id = HomeToolId.PDF_SIGN,
            title = "PDF Sign",
            subtitle = "Draw/reuse signature with templates and multi-placement",
            keywords = "pdf sign signature drag preview templates multiple placements all pages",
            onClick = onOpenPdfSign
        ),
        HomeTool(
            id = HomeToolId.PDF_ANNOTATE,
            title = "PDF Annotate",
            subtitle = "Highlight, text, sticky-note, and freehand drawing",
            keywords = "pdf annotate highlight note freehand draw",
            onClick = onOpenPdfAnnotate
        ),
        HomeTool(
            id = HomeToolId.PDF_PASSWORD,
            title = "PDF Password",
            subtitle = "Add or remove password protection offline",
            keywords = "pdf password protect unlock",
            onClick = onOpenPdfPassword
        ),
        HomeTool(
            id = HomeToolId.PDF_COMPRESS,
            title = "PDF Compress",
            subtitle = "Compress PDFs with quality presets",
            keywords = "pdf compress reduce size",
            onClick = onOpenPdfCompress
        ),
        HomeTool(
            id = HomeToolId.PDF_TEXT,
            title = "PDF to TXT",
            subtitle = "Extract text into a local .txt file",
            keywords = "pdf text txt extract",
            onClick = onOpenPdfText
        ),
        HomeTool(
            id = HomeToolId.PDF_TO_IMAGES,
            title = "PDF to Images",
            subtitle = "Export pages to JPG/PNG/WebP with optional ZIP bundle",
            keywords = "pdf images jpg png webp zip bundle",
            onClick = onOpenPdfToImages
        ),
        HomeTool(
            id = HomeToolId.PDF_BATCH_STAMP,
            title = "Batch Watermark + Bates",
            subtitle = "Apply legal watermark text and Bates numbering across PDFs",
            keywords = "pdf batch watermark bates legal stamp numbering",
            onClick = onOpenPdfBatchStamp
        ),
        HomeTool(
            id = HomeToolId.PDF_OCR,
            title = "Offline OCR",
            subtitle = "Extract text from scans and generate searchable PDFs",
            keywords = "ocr text recognition searchable pdf scan",
            onClick = onOpenPdfOcr
        ),
        HomeTool(
            id = HomeToolId.PDF_FORM,
            title = "AcroForm Fill + Builder",
            subtitle = "Fill existing form fields and add new text fields",
            keywords = "pdf form acroform fill builder fields",
            onClick = onOpenPdfForm
        ),
        HomeTool(
            id = HomeToolId.ID_CARD,
            title = "ID Card / Passport Mode",
            subtitle = "Front+back auto layout to one printable PDF page",
            keywords = "id card passport front back scan pdf",
            onClick = onOpenIdCard
        ),
        HomeTool(
            id = HomeToolId.PDF_TRANSLATE,
            title = "Auto-Translate PDF",
            subtitle = "On-device translation with line-level layout overlays",
            keywords = "translate pdf language offline preserve layout",
            onClick = onOpenPdfTranslate
        ),
        HomeTool(
            id = HomeToolId.PDF_REDACT,
            title = "True PDF Redaction",
            subtitle = "Content-level text removal with irreversible verification",
            keywords = "pdf redact redaction irreversible remove text secure",
            onClick = onOpenPdfRedact
        )
    )
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
