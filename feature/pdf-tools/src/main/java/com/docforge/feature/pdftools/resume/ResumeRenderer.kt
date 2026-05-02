package com.docforge.feature.pdftools.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders a [ResumeData] against a [ResumeTemplate].
 * Pure Compose — can be captured to Bitmap → PDF for export.
 */
@Composable
fun ResumeRenderer(
    template: ResumeTemplate,
    data: ResumeData,
    modifier: Modifier = Modifier
) {
    when (template.layout) {
        TemplateLayout.SINGLE_COLUMN -> SingleColumnResume(template, data, modifier)
        TemplateLayout.TWO_COLUMN -> TwoColumnResume(template, data, modifier)
        TemplateLayout.SIDEBAR_LEFT -> SidebarResume(template, data, sidebarOnLeft = true, modifier = modifier)
        TemplateLayout.SIDEBAR_RIGHT -> SidebarResume(template, data, sidebarOnLeft = false, modifier = modifier)
    }
}

// ── Single Column ──────────────────────────────────────────────────────────────

@Composable
private fun SingleColumnResume(
    t: ResumeTemplate,
    d: ResumeData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(t.backgroundColor)
            .padding(t.pageMargin)
            .verticalScroll(rememberScrollState())
    ) {
        ResumeHeader(t, d)
        Spacer(Modifier.height(t.sectionSpacing))
        if (d.summary.isNotBlank()) {
            SectionTitle(t, "Summary")
            Text(d.summary, fontSize = t.bodyFontSize, color = t.textColor)
            Spacer(Modifier.height(t.sectionSpacing))
        }
        ExperienceSection(t, d.experience)
        EducationSection(t, d.education)
        SkillsSection(t, d.skills)
        if (d.certifications.isNotEmpty()) {
            BulletSection(t, "Certifications", d.certifications)
        }
        if (d.languages.isNotEmpty()) {
            BulletSection(t, "Languages", d.languages)
        }
    }
}

// ── Two Column ─────────────────────────────────────────────────────────────────

@Composable
private fun TwoColumnResume(
    t: ResumeTemplate,
    d: ResumeData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(t.backgroundColor)
            .padding(t.pageMargin)
            .verticalScroll(rememberScrollState())
    ) {
        ResumeHeader(t, d)
        Spacer(Modifier.height(t.sectionSpacing))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                ExperienceSection(t, d.experience)
                EducationSection(t, d.education)
            }
            Column(Modifier.weight(0.8f)) {
                SkillsSection(t, d.skills)
                if (d.certifications.isNotEmpty()) BulletSection(t, "Certifications", d.certifications)
                if (d.languages.isNotEmpty()) BulletSection(t, "Languages", d.languages)
            }
        }
    }
}

// ── Sidebar ────────────────────────────────────────────────────────────────────

@Composable
private fun SidebarResume(
    t: ResumeTemplate,
    d: ResumeData,
    sidebarOnLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(t.backgroundColor)
            .verticalScroll(rememberScrollState())
    ) {
        val sidebar: @Composable ColumnScope.() -> Unit = {
            Column(
                Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .background(t.primaryColor.copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                // Contact
                SectionTitle(t, "Contact")
                if (d.email.isNotBlank()) Text(d.email, fontSize = t.bodyFontSize, color = t.textColor)
                if (d.phone.isNotBlank()) Text(d.phone, fontSize = t.bodyFontSize, color = t.textColor)
                if (d.location.isNotBlank()) Text(d.location, fontSize = t.bodyFontSize, color = t.textColor)
                Spacer(Modifier.height(t.sectionSpacing))
                SkillsSection(t, d.skills)
                if (d.languages.isNotEmpty()) BulletSection(t, "Languages", d.languages)
            }
        }

        val mainContent: @Composable ColumnScope.() -> Unit = {
            Column(
                Modifier
                    .weight(1f)
                    .padding(t.pageMargin)
            ) {
                ResumeHeader(t, d, showContact = false)
                Spacer(Modifier.height(t.sectionSpacing))
                if (d.summary.isNotBlank()) {
                    SectionTitle(t, "Summary")
                    Text(d.summary, fontSize = t.bodyFontSize, color = t.textColor)
                    Spacer(Modifier.height(t.sectionSpacing))
                }
                ExperienceSection(t, d.experience)
                EducationSection(t, d.education)
            }
        }

        if (sidebarOnLeft) {
            Column { sidebar() }
            Column(Modifier.weight(1f)) { mainContent() }
        } else {
            Column(Modifier.weight(1f)) { mainContent() }
            Column { sidebar() }
        }
    }
}

// ── Shared Building Blocks ─────────────────────────────────────────────────────

@Composable
private fun ResumeHeader(
    t: ResumeTemplate,
    d: ResumeData,
    showContact: Boolean = true
) {
    val alignment = when (t.headerStyle) {
        HeaderStyle.TOP_CENTER, HeaderStyle.BANNER -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
    val textAlign = when (t.headerStyle) {
        HeaderStyle.TOP_CENTER, HeaderStyle.BANNER -> TextAlign.Center
        else -> TextAlign.Start
    }

    val headerModifier = if (t.headerStyle == HeaderStyle.BANNER) {
        Modifier
            .fillMaxWidth()
            .background(t.primaryColor, RoundedCornerShape(8.dp))
            .padding(16.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    val nameColor = if (t.headerStyle == HeaderStyle.BANNER) Color.White else t.primaryColor

    Column(modifier = headerModifier, horizontalAlignment = alignment) {
        Text(
            text = d.fullName.ifBlank { "Your Name" },
            fontSize = t.nameFontSize,
            fontWeight = FontWeight.Bold,
            color = nameColor,
            textAlign = textAlign
        )
        if (d.jobTitle.isNotBlank()) {
            Text(d.jobTitle, fontSize = t.sectionTitleFontSize, color = if (t.headerStyle == HeaderStyle.BANNER) Color.White.copy(alpha = 0.85f) else t.accentColor, textAlign = textAlign)
        }
        if (showContact) {
            Spacer(Modifier.height(4.dp))
            val contactParts = listOfNotNull(
                d.email.ifBlank { null },
                d.phone.ifBlank { null },
                d.location.ifBlank { null }
            )
            if (contactParts.isNotEmpty()) {
                Text(
                    contactParts.joinToString("  •  "),
                    fontSize = t.bodyFontSize,
                    color = if (t.headerStyle == HeaderStyle.BANNER) Color.White.copy(alpha = 0.7f) else t.subtitleColor,
                    textAlign = textAlign
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(t: ResumeTemplate, title: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            fontSize = t.sectionTitleFontSize,
            fontWeight = FontWeight.Bold,
            color = t.primaryColor,
            letterSpacing = t.bodyFontSize * 0.1f
        )
        if (t.showDividers) {
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = t.accentColor, thickness = 1.dp)
        }
        Spacer(Modifier.height(t.itemSpacing))
    }
}

@Composable
private fun ExperienceSection(t: ResumeTemplate, entries: List<ExperienceEntry>) {
    if (entries.isEmpty()) return
    SectionTitle(t, "Experience")
    entries.forEach { entry ->
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = t.textColor)) { append(entry.title) }
                append("  —  ")
                withStyle(SpanStyle(color = t.accentColor)) { append(entry.company) }
            },
            fontSize = t.bodyFontSize
        )
        Text(
            "${entry.startDate} – ${entry.endDate}" + if (entry.location.isNotBlank()) "  |  ${entry.location}" else "",
            fontSize = t.bodyFontSize,
            fontStyle = FontStyle.Italic,
            color = t.subtitleColor
        )
        entry.bullets.forEach { bullet ->
            Text("• $bullet", fontSize = t.bodyFontSize, color = t.textColor, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(t.itemSpacing))
    }
    Spacer(Modifier.height(t.sectionSpacing))
}

@Composable
private fun EducationSection(t: ResumeTemplate, entries: List<EducationEntry>) {
    if (entries.isEmpty()) return
    SectionTitle(t, "Education")
    entries.forEach { entry ->
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = t.textColor)) { append(entry.degree) }
                append("  —  ")
                withStyle(SpanStyle(color = t.accentColor)) { append(entry.institution) }
            },
            fontSize = t.bodyFontSize
        )
        Text(entry.year, fontSize = t.bodyFontSize, color = t.subtitleColor, fontStyle = FontStyle.Italic)
        if (entry.details.isNotBlank()) {
            Text(entry.details, fontSize = t.bodyFontSize, color = t.textColor)
        }
        Spacer(Modifier.height(t.itemSpacing))
    }
    Spacer(Modifier.height(t.sectionSpacing))
}

@Composable
private fun SkillsSection(t: ResumeTemplate, skills: List<String>) {
    if (skills.isEmpty()) return
    SectionTitle(t, "Skills")
    // Chip-style flow
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        skills.forEach { skill ->
            Text(
                skill,
                fontSize = t.bodyFontSize,
                color = t.primaryColor,
                modifier = Modifier
                    .border(1.dp, t.accentColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
    Spacer(Modifier.height(t.sectionSpacing))
}

@Composable
private fun BulletSection(t: ResumeTemplate, title: String, items: List<String>) {
    SectionTitle(t, title)
    items.forEach { item ->
        Text("• $item", fontSize = t.bodyFontSize, color = t.textColor)
    }
    Spacer(Modifier.height(t.sectionSpacing))
}
