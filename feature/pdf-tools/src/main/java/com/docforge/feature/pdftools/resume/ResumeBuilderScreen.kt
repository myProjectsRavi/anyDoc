package com.docforge.feature.pdftools.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Pageview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
                title = { Text("Resume Builder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = true }) {
                        Icon(Icons.Default.Pageview, "Preview")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Template Picker ──
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Pageview, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Text("CHOOSE TEMPLATE", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(ResumeTemplates.all) { tmpl ->
                        TemplateCard(
                            template = tmpl,
                            selected = tmpl.id == selectedTemplate.id,
                            onClick = { selectedTemplate = tmpl }
                        )
                    }
                }
            }

            // ── Form Fields ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Text("YOUR DETAILS", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                OutlinedTextField(fullName, { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(jobTitle, { jobTitle = it }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(summary, { summary = it }, label = { Text("Professional Summary") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(skillsText, { skillsText = it }, label = { Text("Skills (comma-separated)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }

            // TODO Phase 6 Stage 2: Experience / Education dynamic list editors

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { showPreview = true },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = CircleShape
                ) {
                    Text("Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Button(
                    onClick = { onExportPdf(selectedTemplate, resumeData) },
                    modifier = Modifier.weight(1f).height(64.dp).shadow(8.dp, CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TemplateCard(
    template: ResumeTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini color swatch preview
        Box(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(template.primaryColor)
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .background(template.accentColor, RoundedCornerShape(topStart = 8.dp))
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            template.name,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            template.category.name.lowercase().replaceFirstChar { it.uppercase() },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                title = { Text("Preview — ${template.name}", fontWeight = FontWeight.Bold) },
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
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onExport,
                        modifier = Modifier.fillMaxWidth().height(64.dp).shadow(8.dp, CircleShape),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = CircleShape
                    ) {
                        Text("Export PDF", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp)
        ) {
            // "Paper" card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(8.dp)
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