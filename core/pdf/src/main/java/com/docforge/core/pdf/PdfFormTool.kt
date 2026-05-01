package com.docforge.core.pdf

import android.content.Context
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDChoice
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDRadioButton
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

enum class PdfFormFieldType {
    TEXT,
    CHECKBOX,
    RADIO,
    CHOICE,
    UNKNOWN
}

data class PdfFormFieldInfo(
    val name: String,
    val type: PdfFormFieldType,
    val value: String,
    val options: List<String>
)

data class PdfFormFillResult(
    val outputFile: File,
    val updatedFieldCount: Int,
    val pageCount: Int,
    val outputSizeBytes: Long
)

data class PdfFormBuildResult(
    val outputFile: File,
    val createdFieldName: String,
    val pageCount: Int,
    val outputSizeBytes: Long
)

class PdfFormTool(
    private val context: Context
) {
    suspend fun listFields(inputUri: Uri): List<PdfFormFieldInfo> = withContext(Dispatchers.IO) {
        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_form_list_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                val form = document.documentCatalog?.acroForm ?: return@use emptyList()
                form.fields.map { field ->
                    PdfFormFieldInfo(
                        name = field.fullyQualifiedName ?: field.partialName ?: "unnamed_field",
                        type = mapFieldType(field),
                        value = field.valueAsString.orEmpty(),
                        options = extractFieldOptions(field)
                    )
                }
            }
        }
    }

    suspend fun fillFields(
        inputUri: Uri,
        outputName: String,
        values: Map<String, String>,
        flatten: Boolean
    ): PdfFormFillResult = withContext(Dispatchers.IO) {
        require(values.isNotEmpty()) { "Enter at least one field update." }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "form_filled_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_form_fill_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                val form = document.documentCatalog?.acroForm
                    ?: error("This PDF does not contain fillable form fields.")
                val coroutineCtx = currentCoroutineContext()

                var updated = 0
                values.forEach { (fieldName, rawValue) ->
                    coroutineCtx.ensureActive()
                    val field = form.getField(fieldName) ?: return@forEach
                    val value = rawValue.trim()
                    when (field) {
                        is PDCheckBox -> {
                            if (value.equals("true", ignoreCase = true) || value == "1" || value.equals("yes", ignoreCase = true)) {
                                field.check()
                            } else {
                                field.unCheck()
                            }
                            updated += 1
                        }

                        is PDRadioButton -> {
                            val candidate = if (value.toIntOrNull() != null) {
                                val idx = value.toInt().coerceAtLeast(0)
                                field.exportValues.getOrNull(idx)
                            } else {
                                value
                            }
                            if (!candidate.isNullOrBlank()) {
                                field.setValue(candidate)
                                updated += 1
                            }
                        }

                        is PDChoice -> {
                            field.setValue(value)
                            updated += 1
                        }

                        else -> {
                            field.setValue(value)
                            updated += 1
                        }
                    }
                }

                form.refreshAppearances()
                if (flatten) {
                    form.flatten()
                }

                document.save(outputFile)
                PdfFormFillResult(
                    outputFile = outputFile,
                    updatedFieldCount = updated,
                    pageCount = document.numberOfPages,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    suspend fun addTextField(
        inputUri: Uri,
        outputName: String,
        fieldName: String,
        pageOneBased: Int,
        xRatio: Float,
        yRatio: Float,
        widthRatio: Float,
        heightRatio: Float,
        defaultValue: String
    ): PdfFormBuildResult = withContext(Dispatchers.IO) {
        val cleanFieldName = fieldName.trim().ifBlank { "field_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "form_builder_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_form_build_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { sourceDoc ->
                require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }
                require(pageOneBased in 1..sourceDoc.numberOfPages) { "Target page is out of bounds." }
                val coroutineCtx = currentCoroutineContext()

                PDDocument().use { outDoc ->
                    repeat(sourceDoc.numberOfPages) { index ->
                        coroutineCtx.ensureActive()
                        importPage(outDoc, sourceDoc.getPage(index))
                    }

                    val acroForm = ensureAcroForm(outDoc)
                    require(acroForm.getField(cleanFieldName) == null) {
                        "Field '$cleanFieldName' already exists. Use a different field name."
                    }

                    val targetPage = outDoc.getPage(pageOneBased - 1)
                    val rect = resolveRect(targetPage, xRatio, yRatio, widthRatio, heightRatio)

                    val textField = PDTextField(acroForm).apply {
                        partialName = cleanFieldName
                        setDefaultValue(defaultValue)
                        setValue(defaultValue)
                    }

                    val widget = PDAnnotationWidget().apply {
                        rectangle = rect
                        page = targetPage
                        setParent(textField)
                        isPrinted = true
                    }
                    textField.widgets = listOf(widget)

                    val pageAnnotations = targetPage.annotations.toMutableList()
                    pageAnnotations += widget
                    targetPage.annotations = pageAnnotations

                    val fields = acroForm.fields.toMutableList()
                    fields += textField
                    acroForm.fields = fields
                    acroForm.refreshAppearances()

                    outDoc.save(outputFile)
                    PdfFormBuildResult(
                        outputFile = outputFile,
                        createdFieldName = cleanFieldName,
                        pageCount = outDoc.numberOfPages,
                        outputSizeBytes = outputFile.length()
                    )
                }
            }
        }
    }

    private fun mapFieldType(field: PDField): PdfFormFieldType {
        return when (field) {
            is PDTextField -> PdfFormFieldType.TEXT
            is PDCheckBox -> PdfFormFieldType.CHECKBOX
            is PDRadioButton -> PdfFormFieldType.RADIO
            is PDChoice -> PdfFormFieldType.CHOICE
            else -> PdfFormFieldType.UNKNOWN
        }
    }

    private fun extractFieldOptions(field: PDField): List<String> {
        return when (field) {
            is PDCheckBox -> listOf(field.onValue)
            is PDRadioButton -> field.exportValues
            is PDChoice -> field.options
            else -> emptyList()
        }
    }

    private fun ensureAcroForm(document: PDDocument): PDAcroForm {
        val existing = document.documentCatalog.acroForm
        if (existing != null) return existing

        val form = PDAcroForm(document)
        val resources = PDResources().apply {
            put(COSName.getPDFName("Helv"), PDType1Font.HELVETICA)
        }
        form.defaultResources = resources
        form.defaultAppearance = "/Helv 10 Tf 0 g"
        form.setNeedAppearances(true)
        document.documentCatalog.acroForm = form
        return form
    }

    private fun resolveRect(
        page: PDPage,
        xRatio: Float,
        yRatio: Float,
        widthRatio: Float,
        heightRatio: Float
    ): PDRectangle {
        val box = page.cropBox ?: page.mediaBox
        val pageWidth = box.width.coerceAtLeast(1f)
        val pageHeight = box.height.coerceAtLeast(1f)

        val width = (pageWidth * widthRatio.coerceIn(0.1f, 0.9f)).coerceAtLeast(80f)
        val height = (pageHeight * heightRatio.coerceIn(0.03f, 0.5f)).coerceAtLeast(18f)

        val maxLeft = (pageWidth - width).coerceAtLeast(0f)
        val maxTop = (pageHeight - height).coerceAtLeast(0f)

        val left = box.lowerLeftX + (maxLeft * xRatio.coerceIn(0f, 1f))
        val topFromTop = maxTop * yRatio.coerceIn(0f, 1f)
        val bottom = (box.lowerLeftY + pageHeight - height - topFromTop).coerceAtLeast(box.lowerLeftY)

        return PDRectangle(left, bottom, width, height)
    }

    private fun importPage(outDoc: PDDocument, sourcePage: PDPage): PDPage {
        val imported = outDoc.importPage(sourcePage)
        imported.rotation = sourcePage.rotation
        imported.mediaBox = sourcePage.mediaBox
        imported.cropBox = sourcePage.cropBox
        imported.resources = sourcePage.resources
        return imported
    }
}
