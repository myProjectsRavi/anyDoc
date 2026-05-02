package com.docforge.feature.pdftools.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeBuilderScreen(
    onBack: () -> Unit,
    onExportPdf: (ResumeTemplate, ResumeData) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTemplate by remember { mutableStateOf(ResumeTemplates.all.first()) }
    var showPreview by rememberSaveable { mutableStateOf(false) }

    // Form state
    var fullName by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var skillsText by rememberSaveable { mutableStateOf("") }

    val resumeData = ResumeData(
        fullName = fullName,
        jobTitle = jobTitle,
        email = email,
        phone = phone,
        location = location,
        summary = summary,
        skills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
    )

    if (showPreview) {
        ResumePreviewScreen(
            template = selectedTemplate,
            data = resumeData,
            onBack = { showPreview = false },
            onExport = { onExportPdf(selectedTemplate, resumeData) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resume Builder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = true }) {
                        Icon(Icons.Default.Preview, "Preview")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Template Picker ──
            Text("Choose Template", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ResumeTemplates.all) { tmpl ->
                    TemplateCard(
                        template = tmpl,
                        selected = tmpl.id == selectedTemplate.id,
                        onClick = { selectedTemplate = tmpl }
                    )
                }
            }

            HorizontalDivider()

            // ── Form Fields ──
            Text("Your Details", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(fullName, { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(jobTitle, { jobTitle = it }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(summary, { summary = it }, label = { Text("Professional Summary") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(skillsText, { skillsText = it }, label = { Text("Skills (comma-separated)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            // TODO Phase 6 Stage 2: Experience / Education dynamic list editors

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showPreview = true }, modifier = Modifier.weight(1f)) {
                    Text("Preview")
                }
                Button(onClick = { onExportPdf(selectedTemplate, resumeData) }, modifier = Modifier.weight(1f)) {
                    Text("Export PDF")
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: ResumeTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) template.primaryColor else Color.Gray.copy(alpha = 0.3f)
    val borderWidth = if (selected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini color swatch preview
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(template.primaryColor)
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .background(template.accentColor, RoundedCornerShape(topStart = 4.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            template.name,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
        Text(
            template.category.name.lowercase().replaceFirstChar { it.uppercase() },
            fontSize = 10.sp,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumePreviewScreen(
    template: ResumeTemplate,
    data: ResumeData,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview — ${template.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onExport) {
                        Text("Export PDF")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFE0E0E0))
                .padding(8.dp)
        ) {
            // "Paper" card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                ResumeRenderer(
                    template = template,
                    data = data,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
