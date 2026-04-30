package com.docforge.feature.pdftools

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AcroForm Fill + Builder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Detect form fields, fill values, and add new text fields for custom form creation.")

        Button(onClick = onPickPdf, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF" else "Replace PDF")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }

        if (state.fields.isNotEmpty()) {
            Text("Detected Fields (Tap to Edit)", fontWeight = FontWeight.SemiBold)
            FieldPicker(
                fields = state.fields,
                selectedFieldName = state.selectedFieldName,
                onFieldSelected = onFieldSelected
            )
        }

        selectedField?.let { field ->
            Text("Structured Editor", fontWeight = FontWeight.SemiBold)
            Text("${field.name} [${field.type}]", style = MaterialTheme.typography.bodySmall)
            when {
                field.type == com.docforge.core.pdf.PdfFormFieldType.CHECKBOX -> {
                    val checked = state.fieldEdits[field.name].orEmpty().equals("true", ignoreCase = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Checked")
                        Switch(
                            checked = checked,
                            onCheckedChange = { onFieldBooleanChanged(field.name, it) }
                        )
                    }
                }

                field.options.isNotEmpty() -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        field.options.forEach { option ->
                            Button(
                                onClick = { onFieldValueChanged(field.name, option) },
                                enabled = !state.isProcessing
                            ) {
                                val selected = if (state.fieldEdits[field.name] == option) "*" else ""
                                Text("$option$selected")
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

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Flatten after fill")
            Switch(checked = state.flattenAfterFill, onCheckedChange = onFlattenChanged)
        }

        Button(onClick = onFillFields, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Apply Field Values")
            }
        }

        Text("Form Builder (Add Text Field)", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = state.builderFieldName, onValueChange = onBuilderFieldNameChanged, label = { Text("Field Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.builderPage, onValueChange = onBuilderPageChanged, label = { Text("Page (1-based)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.builderXRatio, onValueChange = onBuilderXChanged, label = { Text("X Ratio (0-1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.builderYRatio, onValueChange = onBuilderYChanged, label = { Text("Y Ratio (0-1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.builderWidthRatio, onValueChange = onBuilderWidthChanged, label = { Text("Width Ratio (0-1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.builderHeightRatio, onValueChange = onBuilderHeightChanged, label = { Text("Height Ratio (0-1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.builderDefaultValue, onValueChange = onBuilderDefaultChanged, label = { Text("Default Value") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Button(onClick = onAddField, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text("Create Text Field")
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }
        state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
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
            Button(onClick = { onFieldSelected(field.name) }) {
                val selected = if (field.name == selectedFieldName) "*" else ""
                Text("${field.name}$selected")
            }
        }
    }
}
