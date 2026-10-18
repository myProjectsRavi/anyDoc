package com.docforge.feature.pdftools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun BigActionButton(
    text: String,
    onClick: () -> Unit,
    isProcessing: Boolean,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = !isProcessing && enabled,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(8.dp, CircleShape)
    ) {
        if (isProcessing) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Processing...", fontWeight = FontWeight.Black, fontSize = 18.sp)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
            }
            Text(title.uppercase(), color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        content()
    }
}

@Composable
fun PdfSplitRoute(
    viewModel: PdfSplitViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onInputSelected(uri, null)
        }
    }

    PdfSplitScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onOutputBaseNameChanged = viewModel::onOutputBaseNameChanged,
        onStartPageChanged = viewModel::onStartPageChanged,
        onEndPageChanged = viewModel::onEndPageChanged,
        onSplitEveryNChanged = viewModel::onSplitEveryNChanged,
        onExtractPagesChanged = viewModel::onExtractPagesChanged,
        onReorderPagesChanged = viewModel::onReorderPagesChanged,
        onDeletePagesChanged = viewModel::onDeletePagesChanged,
        onRotatePagesChanged = viewModel::onRotatePagesChanged,
        onRotateDegreesChanged = viewModel::onRotateDegreesChanged,
        onSplitRange = viewModel::splitRange,
        onSplitEveryN = viewModel::splitEveryNPages,
        onSplitByBookmarks = viewModel::splitByBookmarks,
        onExtractPages = viewModel::extractPages,
        onReorderPages = viewModel::reorderPages,
        onDeletePages = viewModel::deletePages,
        onRotatePages = viewModel::rotatePages,
        onSaveVisualWorkspace = viewModel::saveVisualWorkspace,
        onClearError = viewModel::clearError
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PdfSplitScreen(
    state: PdfSplitUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onOutputBaseNameChanged: (String) -> Unit,
    onStartPageChanged: (String) -> Unit,
    onEndPageChanged: (String) -> Unit,
    onSplitEveryNChanged: (String) -> Unit,
    onExtractPagesChanged: (String) -> Unit,
    onReorderPagesChanged: (String) -> Unit,
    onDeletePagesChanged: (String) -> Unit,
    onRotatePagesChanged: (String) -> Unit,
    onRotateDegreesChanged: (Int) -> Unit,
    onSplitRange: () -> Unit,
    onSplitEveryN: () -> Unit,
    onSplitByBookmarks: () -> Unit,
    onExtractPages: () -> Unit,
    onReorderPages: () -> Unit,
    onDeletePages: () -> Unit,
    onRotatePages: () -> Unit,
    onSaveVisualWorkspace: (List<Int>, Map<Int, Int>) -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailsError by remember(state.selectedUri) { mutableStateOf<String?>(null) }
    var selectedVisualPages by remember(state.selectedUri) { mutableStateOf<Set<Int>>(emptySet()) }
    var visualOrder by remember(state.selectedUri) { mutableStateOf<List<Int>>(emptyList()) }
    var pendingRotationByPage by remember(state.selectedUri) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var thumbnailQuality by remember { mutableStateOf(PdfSplitThumbnailQuality.MEDIUM) }

    val thumbnailCacheSizeKb = remember {
        ((Runtime.getRuntime().maxMemory() / 1024L) / 8L)
            .toInt()
            .coerceAtLeast(4 * 1024)
    }
    val thumbnailCache = remember(state.selectedUri, thumbnailQuality) {
        BitmapThumbnailLruCache(thumbnailCacheSizeKb)
    }
    var thumbnailCacheVersion by remember(state.selectedUri, thumbnailQuality) {
        mutableStateOf(0)
    }
    val thumbnailLoading = remember(state.selectedUri, thumbnailQuality) {
        mutableStateMapOf<Int, Boolean>()
    }

    LaunchedEffect(state.selectedUri, state.pageCount) {
        val pageCount = state.pageCount ?: 0
        if (state.selectedUri == null || pageCount <= 0) {
            thumbnailsError = null
            selectedVisualPages = emptySet()
            visualOrder = emptyList()
            return@LaunchedEffect
        }

        thumbnailsError = if (pageCount > 120) {
            "Large PDF detected: thumbnails are rendered on-demand for smooth scrolling."
        } else {
            null
        }
        selectedVisualPages = emptySet()
        visualOrder = (1..pageCount).toList()
        pendingRotationByPage = emptyMap()
    }

    DisposableEffect(thumbnailCache) {
        onDispose {
            thumbnailCache.clearAndRecycle()
            thumbnailLoading.clear()
        }
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                Text("Split & Extract", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Split, extract, reorder, delete, rotate, or bookmark-split PDF pages offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(if (state.selectedUri == null) "Upload Document" else "Replace Document", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Tap to browse files", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("Supports PDF", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        // Document List Selected Item
        if (state.inputLabel != null) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SELECTED FILE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
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
                        Text("PDF Document • ${state.pageCount ?: 0} Pages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Tools
        ToolCard("General Settings", Icons.Default.Settings) {
            OutlinedTextField(
                value = state.outputBaseName,
                onValueChange = onOutputBaseNameChanged,
                label = { Text("Output Base Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val selectedPdfUri = state.selectedUri
        val totalPages = state.pageCount
        if (selectedPdfUri != null && totalPages != null && totalPages > 0) {
            val baselineVisualOrder = (1..totalPages).toList()
            val hasPendingWorkspaceEdits = visualOrder != baselineVisualOrder || pendingRotationByPage.isNotEmpty()

            ToolCard("Visual Workspace", Icons.Default.Visibility) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedVisualPages = emptySet() },
                        enabled = !state.isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Clear Selection")
                    }
                    Button(
                        onClick = {
                            visualOrder = baselineVisualOrder
                            pendingRotationByPage = emptyMap()
                            selectedVisualPages = emptySet()
                            onReorderPagesChanged(visualOrder.joinToString(","))
                        },
                        enabled = !state.isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Reset Workspace")
                    }
                    Button(
                        onClick = {
                            val pages = selectedVisualPages.toList().sorted()
                            if (pages.isNotEmpty()) {
                                onExtractPagesChanged(formatPageExpression(pages))
                            }
                        },
                        enabled = !state.isProcessing && selectedVisualPages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Selection -> Extract")
                    }
                    Button(
                        onClick = {
                            val pages = selectedVisualPages.toList().sorted()
                            if (pages.isNotEmpty()) {
                                onDeletePagesChanged(formatPageExpression(pages))
                                val pageSet = pages.toSet()
                                visualOrder = visualOrder.filterNot { page -> pageSet.contains(page) }
                                pendingRotationByPage = pendingRotationByPage.filterKeys { key ->
                                    visualOrder.contains(key)
                                }
                                selectedVisualPages = emptySet()
                            }
                        },
                        enabled = !state.isProcessing && selectedVisualPages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("Queue Delete (0ms)")
                    }
                    Button(
                        onClick = {
                            val pages = selectedVisualPages.toList().sorted()
                            if (pages.isNotEmpty()) {
                                onRotatePagesChanged(formatPageExpression(pages))
                                val updated = pendingRotationByPage.toMutableMap()
                                pages.forEach { page ->
                                    val totalRotation = ((updated[page] ?: 0) + state.rotateDegrees) % 360
                                    if (totalRotation == 0) {
                                        updated.remove(page)
                                    } else {
                                        updated[page] = totalRotation
                                    }
                                }
                                pendingRotationByPage = updated
                            }
                        },
                        enabled = !state.isProcessing && selectedVisualPages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Queue Rotate (0ms)")
                    }
                    Button(
                        onClick = {
                            val order = if (selectedVisualPages.isEmpty()) {
                                visualOrder
                            } else {
                                visualOrder.filter { page -> selectedVisualPages.contains(page) }
                            }
                            if (order.isNotEmpty()) {
                                onReorderPagesChanged(order.joinToString(","))
                            }
                        },
                        enabled = !state.isProcessing && visualOrder.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Visual Order -> Reorder")
                    }
                }

                if (pendingRotationByPage.isNotEmpty()) {
                    Text(
                        "Pending rotations: ${
                            pendingRotationByPage.entries
                                .sortedBy { it.key }
                                .joinToString(", ") { entry -> "p${entry.key}:${entry.value}°" }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text("Thumbnail Quality", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfSplitThumbnailQuality.entries.forEach { quality ->
                        val isSelected = quality == thumbnailQuality
                        Button(
                            onClick = {
                                thumbnailQuality = quality
                                thumbnailsError = if (totalPages > 120) {
                                    "Large PDF detected: thumbnails are rendered on-demand for smooth scrolling."
                                } else {
                                    null
                                }
                            },
                            enabled = !state.isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(quality.label)
                        }
                    }
                }

                val visualIndexByPage = visualOrder.withIndex().associate { it.value to it.index }
                val cacheTick = thumbnailCacheVersion
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = visualOrder, key = { page -> page }) { page ->
                        // Keep item subscribed to LRU cache changes.
                        val cachedThumbnail = remember(page, cacheTick) {
                            thumbnailCache.get(page)
                        }
                        val isLoading = thumbnailLoading[page] == true
                        LaunchedEffect(selectedPdfUri, page, thumbnailQuality, cachedThumbnail) {
                            suspend fun renderPageIfNeeded(targetPage: Int) {
                                if (targetPage !in 1..totalPages) return
                                if (thumbnailCache.get(targetPage) != null) return
                                if (thumbnailLoading[targetPage] == true) return

                                thumbnailLoading[targetPage] = true
                                val rendered = withContext(Dispatchers.IO) {
                                    renderSinglePdfThumbnail(
                                        context = context,
                                        inputUri = selectedPdfUri,
                                        pageOneBased = targetPage,
                                        targetWidthPx = thumbnailQuality.targetWidthPx
                                    )
                                }
                                if (rendered != null) {
                                    val previous = thumbnailCache.put(targetPage, rendered)
                                    if (previous != null && previous !== rendered && !previous.isRecycled) {
                                        previous.recycle()
                                    }
                                    thumbnailCacheVersion += 1
                                } else if (thumbnailsError == null) {
                                    thumbnailsError = "Some thumbnails could not be rendered."
                                }
                                thumbnailLoading.remove(targetPage)
                            }

                            // Render current row and prefetch nearby pages for smoother scrolling.
                            for (targetPage in (page - 2)..(page + 10)) {
                                renderPageIfNeeded(targetPage)
                            }
                        }

                        val selected = selectedVisualPages.contains(page)
                        val pendingRotation = pendingRotationByPage[page] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !state.isProcessing) {
                                    selectedVisualPages = selectedVisualPages.toMutableSet().also { set ->
                                        if (set.contains(page)) set.remove(page) else set.add(page)
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(110.dp)
                                    .fillMaxWidth(0.35f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val thumb = cachedThumbnail
                                if (thumb != null) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = "Page $page thumbnail",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationZ = pendingRotation.toFloat() },
                                        contentScale = ContentScale.FillBounds
                                    )
                                } else {
                                    Text(
                                        text = if (isLoading) "Loading..." else "Preview",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Page $page", fontWeight = FontWeight.SemiBold, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                if (pendingRotation != 0) {
                                    Text(
                                        "Queued rotation: ${pendingRotation}°",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    if (selected) "Selected" else "Tap to select",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val index = visualIndexByPage[page] ?: -1
                                            if (index > 0) {
                                                val updated = visualOrder.toMutableList()
                                                val temp = updated[index - 1]
                                                updated[index - 1] = updated[index]
                                                updated[index] = temp
                                                visualOrder = updated
                                                onReorderPagesChanged(updated.joinToString(","))
                                            }
                                        },
                                        enabled = !state.isProcessing && (visualIndexByPage[page] ?: 0) > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                    ) {
                                        Text("Up")
                                    }
                                    Button(
                                        onClick = {
                                            val index = visualIndexByPage[page] ?: -1
                                            if (index >= 0 && index < visualOrder.lastIndex) {
                                                val updated = visualOrder.toMutableList()
                                                val temp = updated[index + 1]
                                                updated[index + 1] = updated[index]
                                                updated[index] = temp
                                                visualOrder = updated
                                                onReorderPagesChanged(updated.joinToString(","))
                                            }
                                        },
                                        enabled = !state.isProcessing &&
                                            (visualIndexByPage[page] ?: visualOrder.size) < visualOrder.lastIndex,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                    ) {
                                        Text("Down")
                                    }
                                }
                            }
                        }
                    }
                }

                thumbnailsError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (hasPendingWorkspaceEdits) {
                    BigActionButton(
                        text = "Save Workspace Edits",
                        onClick = { onSaveVisualWorkspace(visualOrder, pendingRotationByPage) },
                        isProcessing = state.isProcessing,
                        enabled = visualOrder.isNotEmpty()
                    )
                }
            }
        }

        ToolCard("Split by Range", Icons.Default.ContentCut) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.startPageInput,
                    onValueChange = onStartPageChanged,
                    label = { Text("Start") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.endPageInput,
                    onValueChange = onEndPageChanged,
                    label = { Text("End") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            BigActionButton("Create Range PDF", onSplitRange, state.isProcessing, enabled = state.selectedUri != null)
        }

        ToolCard("Split Every N Pages", Icons.Default.FormatListNumbered) {
            OutlinedTextField(
                value = state.splitEveryNInput,
                onValueChange = onSplitEveryNChanged,
                label = { Text("Pages per split file") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BigActionButton("Create Chunked PDFs", onSplitEveryN, state.isProcessing, enabled = state.selectedUri != null)
        }

        ToolCard("Split by Bookmarks", Icons.Default.Bookmark) {
            Text("Automatically split the PDF at every top-level bookmark.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            BigActionButton("Split by Bookmarks", onSplitByBookmarks, state.isProcessing, enabled = state.selectedUri != null)
        }

        ToolCard("Extract Specific Pages", Icons.Default.FilterCenterFocus) {
            OutlinedTextField(
                value = state.extractPagesInput,
                onValueChange = onExtractPagesChanged,
                label = { Text("Pages (e.g. 1,3,5-7)") },
                modifier = Modifier.fillMaxWidth()
            )
            BigActionButton("Extract Pages", onExtractPages, state.isProcessing, enabled = state.selectedUri != null)
        }

        ToolCard("Reorder Pages", Icons.Default.Reorder) {
            OutlinedTextField(
                value = state.reorderPagesInput,
                onValueChange = onReorderPagesChanged,
                label = { Text("New order (e.g. 3,1,2 or 10-1)") },
                modifier = Modifier.fillMaxWidth()
            )
            BigActionButton("Create Reordered PDF", onReorderPages, state.isProcessing, enabled = state.selectedUri != null)
        }

        ToolCard("Delete Pages", Icons.Default.Delete) {
            OutlinedTextField(
                value = state.deletePagesInput,
                onValueChange = onDeletePagesChanged,
                label = { Text("Pages to remove (e.g. 2,4,8-10)") },
                modifier = Modifier.fillMaxWidth()
            )
            BigActionButton("Delete Pages and Save", onDeletePages, state.isProcessing, enabled = state.selectedUri != null)
        }

        ToolCard("Rotate Pages", Icons.AutoMirrored.Filled.RotateRight) {
            OutlinedTextField(
                value = state.rotatePagesInput,
                onValueChange = onRotatePagesChanged,
                label = { Text("Pages to rotate (e.g. 1,4,7-9)") },
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(90, 180, 270).forEach { degrees ->
                    val isSelected = state.rotateDegrees == degrees
                    Button(
                        onClick = { onRotateDegreesChanged(degrees) },
                        enabled = !state.isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("$degrees°")
                    }
                }
            }
            BigActionButton("Rotate Pages and Save", onRotatePages, state.isProcessing, enabled = state.selectedUri != null)
        }

        // Status & Progress
        if (state.isProcessing || state.statusMessage != null || state.errorMessage != null || state.lastOutputPaths.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isProcessing) {
                    LinearProgressIndicator(
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
                
                if (state.lastOutputPaths.isNotEmpty()) {
                    Text("Outputs Saved:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.lastOutputPaths.forEach { path ->
                        Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("Total size: ${state.lastOutputSizeBytes ?: 0} bytes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
    }
}

private enum class PdfSplitThumbnailQuality(
    val label: String,
    val targetWidthPx: Int
) {
    LOW("Low", 140),
    MEDIUM("Medium", 220),
    HIGH("High", 320)
}

private class BitmapThumbnailLruCache(maxSizeKb: Int) : LruCache<Int, Bitmap>(maxSizeKb) {
    override fun sizeOf(key: Int, value: Bitmap): Int {
        return (value.byteCount / 1024).coerceAtLeast(1)
    }

    override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
        if (evicted && oldValue !== newValue && !oldValue.isRecycled) {
            oldValue.recycle()
        }
    }

    fun clearAndRecycle() {
        val toRecycle = snapshot().values.toList()
        evictAll()
        toRecycle.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}

private fun renderSinglePdfThumbnail(
    context: Context,
    inputUri: Uri,
    pageOneBased: Int,
    targetWidthPx: Int
): Bitmap? {
    return runCatching {
        context.contentResolver.openFileDescriptor(inputUri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) {
                    null
                } else {
                    val pageIndex = (pageOneBased - 1).coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(pageIndex).use { page ->
                        val sourceWidth = page.width.coerceAtLeast(1)
                        val sourceHeight = page.height.coerceAtLeast(1)
                        val scale = targetWidthPx.toFloat() / sourceWidth.toFloat()
                        val width = targetWidthPx.coerceAtLeast(1)
                        val height = (sourceHeight * scale).toInt().coerceAtLeast(1)
                        Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).also { bitmap ->
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        }
    }.getOrNull()
}

private fun formatPageExpression(pagesAscending: List<Int>): String {
    if (pagesAscending.isEmpty()) return ""
    val pages = pagesAscending.distinct().sorted()
    val chunks = mutableListOf<String>()
    var rangeStart = pages.first()
    var previous = pages.first()

    for (i in 1 until pages.size) {
        val value = pages[i]
        if (value == previous + 1) {
            previous = value
            continue
        }
        chunks += if (rangeStart == previous) {
            rangeStart.toString()
        } else {
            "$rangeStart-$previous"
        }
        rangeStart = value
        previous = value
    }

    chunks += if (rangeStart == previous) {
        rangeStart.toString()
    } else {
        "$rangeStart-$previous"
    }
    return chunks.joinToString(",")
}
