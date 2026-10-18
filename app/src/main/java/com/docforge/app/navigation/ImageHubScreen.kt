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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Image Hub - TinyWow-inspired categorized view of ALL image tools
 * Simple grid layout with color-coded operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageHubScreen(
    onBack: () -> Unit,
    onOpenImageConverter: () -> Unit,
    onOpenImageCompressor: () -> Unit,
    onOpenImageResize: () -> Unit,
    onOpenImageCrop: () -> Unit,
    onOpenImageFlip: () -> Unit,
    onOpenImageRotate: () -> Unit,
    onOpenImageToPdf: () -> Unit,
    onOpenImageRoundCorners: () -> Unit,
    onOpenImageBorder: () -> Unit,
    onOpenImageBrightness: () -> Unit,
    onOpenScanner: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Image Tools", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(
                            "11 powerful tools • 100% offline",
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
            items(buildImageTools(
                onOpenImageConverter,
                onOpenImageCompressor,
                onOpenImageResize,
                onOpenImageCrop,
                onOpenImageFlip,
                onOpenImageRotate,
                onOpenImageToPdf,
                onOpenImageRoundCorners,
                onOpenImageBorder,
                onOpenImageBrightness,
                onOpenScanner
            )) { tool ->
                ImageToolCard(
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
private fun ImageToolCard(
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

private data class ImageToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

private fun buildImageTools(
    onOpenImageConverter: () -> Unit,
    onOpenImageCompressor: () -> Unit,
    onOpenImageResize: () -> Unit,
    onOpenImageCrop: () -> Unit,
    onOpenImageFlip: () -> Unit,
    onOpenImageRotate: () -> Unit,
    onOpenImageToPdf: () -> Unit,
    onOpenImageRoundCorners: () -> Unit,
    onOpenImageBorder: () -> Unit,
    onOpenImageBrightness: () -> Unit,
    onOpenScanner: () -> Unit
): List<ImageToolItem> = listOf(
    ImageToolItem("Convert", "JPG, PNG, WebP, HEIC", Icons.Default.SyncAlt, Color(0xFF6366F1), onOpenImageConverter),
    ImageToolItem("Compress", "Reduce file size", Icons.Default.Compress, Color(0xFF06B6D4), onOpenImageCompressor),
    ImageToolItem("Resize", "Change dimensions", Icons.Default.PhotoSizeSelectLarge, Color(0xFF10B981), onOpenImageResize),
    ImageToolItem("Crop", "Trim image", Icons.Default.Crop, Color(0xFF059669), onOpenImageCrop),
    ImageToolItem("Flip", "Mirror image", Icons.Default.Flip, Color(0xFF8B5CF6), onOpenImageFlip),
    ImageToolItem("Rotate", "Change angle", Icons.Default.RotateRight, Color(0xFF7C3AED), onOpenImageRotate),
    ImageToolItem("Image to PDF", "Create document", Icons.Default.PictureAsPdf, Color(0xFFE11D48), onOpenImageToPdf),
    ImageToolItem("Round Corners", "Smooth edges", Icons.Default.RoundedCorner, Color(0xFFEC4899), onOpenImageRoundCorners),
    ImageToolItem("Add Border", "Frame image", Icons.Default.BorderOuter, Color(0xFF0EA5E9), onOpenImageBorder),
    ImageToolItem("Adjust Light", "Brightness/contrast", Icons.Default.Brightness6, Color(0xFFF59E0B), onOpenImageBrightness),
    ImageToolItem("Scanner", "Capture documents", Icons.Default.CameraAlt, Color(0xFF16A34A), onOpenScanner)
)
