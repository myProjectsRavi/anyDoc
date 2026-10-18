package com.docforge.feature.history

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.domain.model.ConversionRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    HistoryScreen(state.records, paddingValues)
}

@Composable
fun HistoryScreen(
    records: List<ConversionRecord>,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("Recent Conversions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Tap any item to open the file.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (records.isEmpty()) {
            Text("No conversion history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(records) { record ->
                    HistoryItem(record, onClick = { openOutputFile(context, record.outputPath) })
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(record: ConversionRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.operation, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(record.sourceLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(record.createdAtMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text("Output: ${record.outputPath}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

private fun openOutputFile(context: android.content.Context, outputPath: String) {
    if (outputPath.isBlank()) {
        Toast.makeText(context, "No output file recorded", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri: Uri = if (outputPath.startsWith("content://")) {
            Uri.parse(outputPath)
        } else {
            val file = File(outputPath)
            if (!file.exists()) {
                Toast.makeText(context, "File no longer exists", Toast.LENGTH_SHORT).show()
                return
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        val mime = guessMimeType(outputPath)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun guessMimeType(path: String): String {
    val lower = path.lowercase(Locale.ROOT).substringAfterLast('.', "")
    return when (lower) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "rtf" -> "application/rtf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "tif", "tiff" -> "image/tiff"
        "heic", "heif" -> "image/heic"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "zip" -> "application/zip"
        else -> "*/*"
    }
}
