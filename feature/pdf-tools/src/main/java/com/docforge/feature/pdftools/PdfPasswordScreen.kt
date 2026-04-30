package com.docforge.feature.pdftools

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PdfPasswordRoute(
    viewModel: PdfPasswordViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val label = readLabel(context, uri)
            viewModel.onInputSelected(uri, label)
        }
    }

    PdfPasswordScreen(
        state = state,
        paddingValues = paddingValues,
        onPickPdf = { picker.launch("application/pdf") },
        onModeChanged = viewModel::onModeChanged,
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onUserPasswordChanged = viewModel::onUserPasswordChanged,
        onOwnerPasswordChanged = viewModel::onOwnerPasswordChanged,
        onUnlockPasswordChanged = viewModel::onUnlockPasswordChanged,
        onApply = viewModel::applyPasswordAction,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfPasswordScreen(
    state: PdfPasswordUiState,
    paddingValues: PaddingValues,
    onPickPdf: () -> Unit,
    onModeChanged: (PdfPasswordMode) -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onUserPasswordChanged: (String) -> Unit,
    onOwnerPasswordChanged: (String) -> Unit,
    onUnlockPasswordChanged: (String) -> Unit,
    onApply: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("PDF Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Add or remove PDF password protection fully offline.")

        Button(onClick = onPickPdf, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF" else "Replace PDF")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }
        state.isEncrypted?.let { encrypted ->
            Text(
                text = if (encrypted) "Current state: Password protected" else "Current state: Not protected",
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Mode", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PdfPasswordMode.entries.forEach { mode ->
                Button(
                    onClick = { onModeChanged(mode) },
                    enabled = !state.isProcessing
                ) {
                    val selected = if (state.mode == mode) "*" else ""
                    Text("${mode.name}$selected")
                }
            }
        }

        if (state.mode == PdfPasswordMode.PROTECT) {
            OutlinedTextField(
                value = state.userPasswordInput,
                onValueChange = onUserPasswordChanged,
                label = { Text("User Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.ownerPasswordInput,
                onValueChange = onOwnerPasswordChanged,
                label = { Text("Owner Password (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = state.unlockPasswordInput,
                onValueChange = onUnlockPasswordChanged,
                label = { Text("Current PDF Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(onClick = onApply, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(if (state.mode == PdfPasswordMode.PROTECT) "Protect PDF" else "Unlock PDF")
            }
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
