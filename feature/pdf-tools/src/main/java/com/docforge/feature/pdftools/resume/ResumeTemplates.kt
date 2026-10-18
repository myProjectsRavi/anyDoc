package com.docforge.feature.pdftools.resume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 10 bundled resume templates covering all categories.
 */
object ResumeTemplates {

    val all: List<ResumeTemplate> by lazy {
        listOf(
            modernEngineer,
            classicExecutive,
            minimalDesigner,
            twoColumnManager,
            academicResearcher,
            boldSales,
            cleanGeneral,
            creativeSidebar,
            compactTech,
            elegantProfessional
        )
    }

    /** 1 — Modern Engineer: clean single-column, blue accent */
    val modernEngineer = ResumeTemplate(
        id = "modern_engineer",
        name = "Modern Engineer",
        category = TemplateCategory.ENGINEER,
        style = TemplateStyle.MODERN,
        layout = TemplateLayout.SINGLE_COLUMN,
        primaryColor = Color(0xFF1565C0),
        accentColor = Color(0xFF42A5F5),
        showDividers = true,
        showSectionIcons = true,
        headerStyle = HeaderStyle.BANNER
    )

    /** 2 — Classic Executive: serif feel, dark navy */
    val classicExecutive = ResumeTemplate(
        id = "classic_executive",
        name = "Classic Executive",
        category = TemplateCategory.MANAGER,
        style = TemplateStyle.EXECUTIVE,
        layout = TemplateLayout.SINGLE_COLUMN,
        primaryColor = Color(0xFF0D1B2A),
        accentColor = Color(0xFF415A77),
        nameFontSize = 32.sp,
        sectionTitleFontSize = 18.sp,
        bodyFontSize = 13.sp,
        showDividers = true,
        headerStyle = HeaderStyle.TOP_CENTER
    )

    /** 3 — Minimal Designer: ultra-clean, lots of whitespace */
    val minimalDesigner = ResumeTemplate(
        id = "minimal_designer",
        name = "Minimal Designer",
        category = TemplateCategory.DESIGNER,
        style = TemplateStyle.MINIMAL,
        layout = TemplateLayout.SINGLE_COLUMN,
        primaryColor = Color(0xFF212121),
        accentColor = Color(0xFF757575),
        nameFontSize = 24.sp,
        sectionSpacing = 24.dp,
        showDividers = false,
        showSectionIcons = false,
        headerStyle = HeaderStyle.TOP_LEFT
    )

    /** 4 — Two-Column Manager: sidebar for skills, main for experience */
    val twoColumnManager = ResumeTemplate(
        id = "two_column_manager",
        name = "Two-Column Manager",
        category = TemplateCategory.MANAGER,
        style = TemplateStyle.MODERN,
        layout = TemplateLayout.SIDEBAR_LEFT,
        primaryColor = Color(0xFF1B5E20),
        accentColor = Color(0xFF66BB6A),
        backgroundColor = Color.White,
        showDividers = true,
        showSectionIcons = true,
        headerStyle = HeaderStyle.SIDEBAR_HEADER
    )

    /** 5 — Academic Researcher: traditional, serif-friendly */
    val academicResearcher = ResumeTemplate(
        id = "academic_researcher",
        name = "Academic Researcher",
        category = TemplateCategory.ACADEMIC,
        style = TemplateStyle.CLASSIC,
        layout = TemplateLayout.SINGLE_COLUMN,
        primaryColor = Color(0xFF4A148C),
        accentColor = Color(0xFF7E57C2),
        nameFontSize = 26.sp,
        sectionTitleFontSize = 15.sp,
        bodyFontSize = 12.sp,
        sectionSpacing = 14.dp,
        showDividers = true,
        headerStyle = HeaderStyle.TOP_LEFT
    )

    /** 6 — Bold Sales: high-contrast, energetic */
    val boldSales = ResumeTemplate(
        id = "bold_sales",
        name = "Bold Sales",
        category = TemplateCategory.SALES,
        style = TemplateStyle.CREATIVE,
        layout = TemplateLayout.SINGLE_COLUMN,
        primaryColor = Color(0xFFE65100),
        accentColor = Color(0xFFFF9800),
        nameFontSize = 30.sp,
        showDividers = true,
        showSectionIcons = true,
        headerStyle = HeaderStyle.BANNER
    )

    /** 7 — Clean General: safe for any role */
    val cleanGeneral = ResumeTemplate(
        id = "clean_general",
        name = "Clean General",
        category = TemplateCategory.GENERAL,
        style = TemplateStyle.MODERN,
        layout = TemplateLayout.SINGLE_COLUMN,
        primaryColor = Color(0xFF37474F),
        accentColor = Color(0xFF78909C),
        showDividers = true,
        headerStyle = HeaderStyle.TOP_LEFT
    )

    /** 8 — Creative Sidebar: right sidebar for contact/skills */
    val creativeSidebar = ResumeTemplate(
        id = "creative_sidebar",
        name = "Creative Sidebar",
        category = TemplateCategory.DESIGNER,
        style = TemplateStyle.CREATIVE,
        layout = TemplateLayout.SIDEBAR_RIGHT,
        primaryColor = Color(0xFFAD1457),
        accentColor = Color(0xFFEC407A),
        backgroundColor = Color(0xFFFCE4EC),
        showDividers = false,
        showSectionIcons = true,
        headerStyle = HeaderStyle.TOP_LEFT
    )

    /** 9 — Compact Tech: dense, two-column, info-packed */
    val compactTech = ResumeTemplate(
        id = "compact_tech",
        name = "Compact Tech",
        category = TemplateCategory.ENGINEER,
        style = TemplateStyle.MINIMAL,
        layout = TemplateLayout.TWO_COLUMN,
        primaryColor = Color(0xFF006064),
        accentColor = Color(0xFF00ACC1),
        nameFontSize = 24.sp,
        bodyFontSize = 11.sp,
        sectionSpacing = 12.dp,
        itemSpacing = 6.dp,
        pageMargin = 16.dp,
        showDividers = true,
        headerStyle = HeaderStyle.TOP_CENTER
    )

    /** 10 — Elegant Professional: warm tones, balanced */
    val elegantProfessional = ResumeTemplate(
        id = "elegant_professional",
        name = "Elegant Professional",
        category = TemplateCategory.GENERAL,
        style = TemplateStyle.EXECUTIVE,
        layout = TemplateLayout.SINGLE_COLUMN,
        primaryColor = Color(0xFF3E2723),
        accentColor = Color(0xFF8D6E63),
        nameFontSize = 28.sp,
        sectionTitleFontSize = 16.sp,
        showDividers = true,
        headerStyle = HeaderStyle.TOP_CENTER
    )
}
