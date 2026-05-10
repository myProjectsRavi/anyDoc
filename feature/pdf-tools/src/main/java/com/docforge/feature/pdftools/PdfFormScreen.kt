package com.docforge.feature.pdftools

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PdfFormRoute(
    viewModel: PdfFormViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onInputSelected(uri, readLabel(context, uri))
        }
    }

    PdfFormScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onFieldSelected = viewModel::onFieldSelected,
        onFieldValueChanged = viewModel::onFieldValueChanged,
        onFieldBooleanChanged = viewModel::onFieldBooleanChanged,
        onFlattenChanged = viewModel::onFlattenChanged,
        onBuilderFieldNameChanged = viewModel::onBuilderFieldNameChanged,
        onBuilderPageChanged = viewModel::onBuilderPageChanged,
        onBuilderXChanged = viewModel::onBuilderXChanged,
        onBuilderYChanged = viewModel::onBuilderYChanged,
        onBuilderWidthChanged = viewModel::onBuilderWidthChanged,
        onBuilderHeightChanged = viewModel::onBuilderHeightChanged,
        onBuilderDefaultChanged = viewModel::onBuilderDefaultChanged,
        onFillFields = viewModel::fillFields,
        onAddField = viewModel::addTextField,
        onClearError = viewModel::clearError
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PdfFormScreen(
    state: PdfFormUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onFieldSelected: (String) -> Unit,
    onFieldValueChanged: (String, String) -> Unit,
    onFieldBooleanChanged: (String, Boolean) -> Unit,
    onFlattenChanged: (Boolean) -> Unit,
    onBuilderFieldNameChanged: (String) -> Unit,
    onBuilderPageChanged: (String) -> Unit,
    onBuilderXChanged: (String) -> Unit,
    onBuilderYChanged: (String) -> Unit,
    onBuilderWidthChanged: (String) -> Unit,
    onBuilderHeightChanged: (String) -> Unit,
    onBuilderDefaultChanged: (String) -> Unit,
    onFillFields: () -> Unit,
    onAddField: () -> Unit,
    onClearError: () -> Unit
) {
    val selectedField = state.fields.firstOrNull { it.name == state.selectedFieldName }

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
                Icon(Icons.Default.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("AcroForm Fill", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("Detect form fields, fill values, and add new text fields for custom form creation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Text("Upload Document", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Tap to browse files", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }

            if (state.inputLabel != null) {
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
                        Text("PDF Document", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { /* Remove */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            if (state.fields.isNotEmpty()) {
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
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Text("DETECTED FIELDS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    FieldPicker(
                        fields = state.fields,
                        selectedFieldName = state.selectedFieldName,
                        onFieldSelected = onFieldSelected
                    )

                    selectedField?.let { field ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Edit ${field.name} [${field.type}]", fontWeight = FontWeight.SemiBold)
                        when {
                            field.type == com.docforge.core.pdf.PdfFormFieldType.CHECKBOX -> {
                                val checked = state.fieldEdits[field.name].orEmpty().equals("true", ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable { onFieldBooleanChanged(field.name, !checked) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Checked", fontWeight = FontWeight.Medium)
                                    Switch(
                                        checked = checked,
                                        onCheckedChange = { onFieldBooleanChanged(field.name, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                }
                            }

                            field.options.isNotEmpty() -> {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    field.options.forEach { option ->
                                        val isSelected = state.fieldEdits[field.name] == option
                                        Button(
                                            onClick = { onFieldValueChanged(field.name, option) },
                                            enabled = !state.isProcessing,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text(option)
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = state.fieldEdits[field.name].orEmpty(),
                                    onValueChange = { onFieldValueChanged(field.name, it) },
                                    label = { Text("Selected Value") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            else -> {
                                OutlinedTextField(
                                    value = state.fieldEdits[field.name].orEmpty(),
                                    onValueChange = { onFieldValueChanged(field.name, it) },
                                    label = { Text("Field Value") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

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
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Text("FORM BUILDER", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.builderFieldName, onValueChange = onBuilderFieldNameChanged, label = { Text("Field Name") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = state.builderPage, onValueChange = onBuilderPageChanged, label = { Text("Page (1-based)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.builderXRatio, onValueChange = onBuilderXChanged, label = { Text("X Ratio") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = state.builderYRatio, onValueChange = onBuilderYChanged, label = { Text("Y Ratio") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.builderWidthRatio, onValueChange = onBuilderWidthChanged, label = { Text("Width") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = state.builderHeightRatio, onValueChange = onBuilderHeightChanged, label = { Text("Height") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = state.builderDefaultValue, onValueChange = onBuilderDefaultChanged, label = { Text("Default Value") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Button(onClick = onAddField, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Create Text Field")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = state.outputName,
                    onValueChange = onOutputNameChanged,
                    label = { Text("Output File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onFlattenChanged(!state.flattenAfterFill) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Flatten after fill", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = state.flattenAfterFill,
                        onCheckedChange = onFlattenChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            val rotation by animateFloatAsState(
                targetValue = if (state.isProcessing) 360f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )
            Button(
                onClick = onFillFields,
                enabled = !state.isProcessing && state.inputLabel != null,
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
                    Text("Apply & Export", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            if (state.statusMessage != null || state.errorMessage != null || state.lastOutputPath != null) {
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
                }
            }
        }
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FieldPicker(
    fields: List<com.docforge.core.pdf.PdfFormFieldInfo>,
    selectedFieldName: String?,
    onFieldSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fields.forEach { field ->
            val isSelected = field.name == selectedFieldName
            Button(
                onClick = { onFieldSelected(field.name) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(field.name)
            }
        }
    }
}
