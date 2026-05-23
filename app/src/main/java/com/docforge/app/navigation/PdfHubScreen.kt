package com.docforge.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PDF Hub - TinyWow-inspired categorized view of ALL PDF tools
 * Clean grid layout with color-coded categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfHubScreen(
    onBack: () -> Unit,
    onOpenPdfMerge: () -> Unit,
    onOpenPdfSplit: () -> Unit,
    onOpenPdfCompress: () -> Unit,
    onOpenPdfToImages: () -> Unit,
    onOpenImageToPdf: () -> Unit,
    onOpenPdfPassword: () -> Unit,
    onOpenPdfUnlock: () -> Unit,
    onOpenDocToPdf: () -> Unit,
    onOpenPdfRotate: () -> Unit,
    onOpenPdfDelete: () -> Unit,
    onOpenPdfPageNumbers: () -> Unit,
    onOpenPdfWatermark: () -> Unit,
    onOpenPdfCrop: () -> Unit,
    onOpenPdfOrganize: () -> Unit,
    onOpenPdfSign: () -> Unit,
    onOpenPdfRepair: () -> Unit,
    onOpenPdfFlatten: () -> Unit,
    onOpenPdfMetadata: () -> Unit,
    onOpenPdfGrayscale: () -> Unit,
    onOpenPdfOcr: () -> Unit,
    onOpenPdfRedact: () -> Unit,
    onOpenPdfAnnotate: () -> Unit,
    onOpenPdfForm: () -> Unit,
    onOpenPdfTranslate: () -> Unit,
    onOpenPdfCompare: () -> Unit,
    onOpenPdfHeaderFooter: () -> Unit,
    onOpenResumeBuilder: () -> Unit,
    onOpenIdCard: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PDF Tools", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(
                            "28 powerful tools • 100% offline",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(buildPdfTools(
                onOpenPdfMerge, onOpenPdfSplit, onOpenPdfCompress, onOpenPdfToImages,
                onOpenImageToPdf, onOpenPdfPassword, onOpenPdfUnlock, onOpenDocToPdf,
                onOpenPdfRotate, onOpenPdfDelete, onOpenPdfPageNumbers, onOpenPdfWatermark,
                onOpenPdfCrop, onOpenPdfOrganize, onOpenPdfSign, onOpenPdfRepair,
                onOpenPdfFlatten, onOpenPdfMetadata, onOpenPdfGrayscale, onOpenPdfOcr,
                onOpenPdfRedact, onOpenPdfAnnotate, onOpenPdfForm, onOpenPdfTranslate,
                onOpenPdfCompare, onOpenPdfHeaderFooter, onOpenResumeBuilder, onOpenIdCard
            )) { tool ->
                ToolCard(
                    title = tool.title,
                    subtitle = tool.subtitle,
                    icon = tool.icon,
                    color = tool.color,
                    onClick = tool.onClick
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private data class PdfToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

private fun buildPdfTools(
    onOpenPdfMerge: () -> Unit,
    onOpenPdfSplit: () -> Unit,
    onOpenPdfCompress: () -> Unit,
    onOpenPdfToImages: () -> Unit,
    onOpenImageToPdf: () -> Unit,
    onOpenPdfPassword: () -> Unit,
    onOpenPdfUnlock: () -> Unit,
    onOpenDocToPdf: () -> Unit,
    onOpenPdfRotate: () -> Unit,
    onOpenPdfDelete: () -> Unit,
    onOpenPdfPageNumbers: () -> Unit,
    onOpenPdfWatermark: () -> Unit,
    onOpenPdfCrop: () -> Unit,
    onOpenPdfOrganize: () -> Unit,
    onOpenPdfSign: () -> Unit,
    onOpenPdfRepair: () -> Unit,
    onOpenPdfFlatten: () -> Unit,
    onOpenPdfMetadata: () -> Unit,
    onOpenPdfGrayscale: () -> Unit,
    onOpenPdfOcr: () -> Unit,
    onOpenPdfRedact: () -> Unit,
    onOpenPdfAnnotate: () -> Unit,
    onOpenPdfForm: () -> Unit,
    onOpenPdfTranslate: () -> Unit,
    onOpenPdfCompare: () -> Unit,
    onOpenPdfHeaderFooter: () -> Unit,
    onOpenResumeBuilder: () -> Unit,
    onOpenIdCard: () -> Unit
): List<PdfToolItem> = listOf(
    PdfToolItem("Merge PDF", "Combine multiple files", Icons.Default.MergeType, Color(0xFF6366F1), onOpenPdfMerge),
    PdfToolItem("Split PDF", "Extract pages", Icons.Default.CallSplit, Color(0xFF8B5CF6), onOpenPdfSplit),
    PdfToolItem("Compress PDF", "Reduce file size", Icons.Default.Compress, Color(0xFF06B6D4), onOpenPdfCompress),
    PdfToolItem("PDF to JPG", "Export as images", Icons.Default.Image, Color(0xFF10B981), onOpenPdfToImages),
    PdfToolItem("JPG to PDF", "Create from images", Icons.Default.PhotoLibrary, Color(0xFF14B8A6), onOpenImageToPdf),
    PdfToolItem("Protect PDF", "Add password", Icons.Default.Lock, Color(0xFFEF4444), onOpenPdfPassword),
    PdfToolItem("Unlock PDF", "Remove password", Icons.Default.LockOpen, Color(0xFFF59E0B), onOpenPdfUnlock),
    PdfToolItem("Word to PDF", "Convert documents", Icons.Default.Description, Color(0xFF3B82F6), onOpenDocToPdf),
    PdfToolItem("Rotate PDF", "Change orientation", Icons.Default.RotateRight, Color(0xFF8B5CF6), onOpenPdfRotate),
    PdfToolItem("Delete Pages", "Remove pages", Icons.Default.DeleteSweep, Color(0xFFDC2626), onOpenPdfDelete),
    PdfToolItem("Page Numbers", "Add numbering", Icons.Default.Checklist, Color(0xFF0EA5E9), onOpenPdfPageNumbers),
    PdfToolItem("Watermark", "Add branding", Icons.Default.WaterDrop, Color(0xFF6366F1), onOpenPdfWatermark),
    PdfToolItem("Crop PDF", "Trim margins", Icons.Default.CropFree, Color(0xFF059669), onOpenPdfCrop),
    PdfToolItem("Organize PDF", "Reorder pages", Icons.Default.Reorder, Color(0xFF7C3AED), onOpenPdfOrganize),
    PdfToolItem("Sign PDF", "Add signature", Icons.Default.Draw, Color(0xFFEC4899), onOpenPdfSign),
    PdfToolItem("Repair PDF", "Fix corruption", Icons.Default.Build, Color(0xFFF59E0B), onOpenPdfRepair),
    PdfToolItem("Flatten PDF", "Remove editing", Icons.Default.Layers, Color(0xFF64748B), onOpenPdfFlatten),
    PdfToolItem("Metadata", "Edit properties", Icons.Default.Info, Color(0xFF475569), onOpenPdfMetadata),
    PdfToolItem("Grayscale", "Black & white", Icons.Default.FilterBAndW, Color(0xFF6B7280), onOpenPdfGrayscale),
    PdfToolItem("OCR", "Text recognition", Icons.Default.TextFields, Color(0xFF16A34A), onOpenPdfOcr),
    PdfToolItem("Redact PDF", "Hide sensitive data", Icons.Default.VisibilityOff, Color(0xFFDC2626), onOpenPdfRedact),
    PdfToolItem("Annotate", "Add notes", Icons.Default.Edit, Color(0xFFF97316), onOpenPdfAnnotate),
    PdfToolItem("Fill Forms", "Complete PDFs", Icons.Default.Assignment, Color(0xFF0891B2), onOpenPdfForm),
    PdfToolItem("Translate", "Change language", Icons.Default.Translate, Color(0xFF7C3AED), onOpenPdfTranslate),
    PdfToolItem("Compare PDF", "Find differences", Icons.Default.Compare, Color(0xFF8B5CF6), onOpenPdfCompare),
    PdfToolItem("Header/Footer", "Add text", Icons.Default.Notes, Color(0xFF0EA5E9), onOpenPdfHeaderFooter),
    PdfToolItem("Resume Builder", "Professional CVs", Icons.Default.Work, Color(0xFF10B981), onOpenResumeBuilder),
    PdfToolItem("ID Card Sheet", "Print front+back", Icons.Default.CreditCard, Color(0xFFEC4899), onOpenIdCard)
)
