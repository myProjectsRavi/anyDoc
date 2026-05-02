package com.docforge.feature.pdftools.resume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A resume template definition. Each template is a pure data description
 * that the ResumeRenderer composable interprets to produce a visual layout.
 */
data class ResumeTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val style: TemplateStyle,
    val layout: TemplateLayout = TemplateLayout.SINGLE_COLUMN,
    val primaryColor: Color = Color(0xFF1A237E),
    val accentColor: Color = Color(0xFF0277BD),
    val backgroundColor: Color = Color.White,
    val textColor: Color = Color(0xFF212121),
    val subtitleColor: Color = Color(0xFF616161),
    val fontFamily: FontFamily = FontFamily.Default,
    val nameFontSize: TextUnit = 28.sp,
    val sectionTitleFontSize: TextUnit = 16.sp,
    val bodyFontSize: TextUnit = 12.sp,
    val sectionSpacing: Dp = 16.dp,
    val itemSpacing: Dp = 8.dp,
    val pageMargin: Dp = 24.dp,
    val showDividers: Boolean = true,
    val showSectionIcons: Boolean = false,
    val headerStyle: HeaderStyle = HeaderStyle.TOP_LEFT,
    val thumbnailAsset: String? = null // WebP thumbnail path in assets
)

enum class TemplateCategory {
    ENGINEER,
    DESIGNER,
    MANAGER,
    ACADEMIC,
    SALES,
    GENERAL
}

enum class TemplateStyle {
    MODERN,
    CLASSIC,
    MINIMAL,
    CREATIVE,
    EXECUTIVE
}

enum class TemplateLayout {
    SINGLE_COLUMN,
    TWO_COLUMN,
    SIDEBAR_LEFT,
    SIDEBAR_RIGHT
}

enum class HeaderStyle {
    TOP_LEFT,
    TOP_CENTER,
    BANNER,
    SIDEBAR_HEADER
}

/**
 * User-provided resume data that gets rendered against a template.
 */
data class ResumeData(
    val fullName: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val summary: String = "",
    val experience: List<ExperienceEntry> = emptyList(),
    val education: List<EducationEntry> = emptyList(),
    val skills: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val links: List<LinkEntry> = emptyList()
)

data class ExperienceEntry(
    val title: String,
    val company: String,
    val location: String = "",
    val startDate: String,
    val endDate: String = "Present",
    val bullets: List<String> = emptyList()
)

data class EducationEntry(
    val degree: String,
    val institution: String,
    val year: String,
    val details: String = ""
)

data class LinkEntry(
    val label: String,
    val url: String
)
