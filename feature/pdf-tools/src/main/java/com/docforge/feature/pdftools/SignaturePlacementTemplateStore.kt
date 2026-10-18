package com.docforge.feature.pdftools

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SignaturePlacementTemplateMeta(
    val name: String,
    val placementCount: Int,
    val updatedAtMillis: Long
)

class SignaturePlacementTemplateStore(
    context: Context
) {
    private val appContext = context.applicationContext
    private val templatesFile = File(appContext.filesDir, "pdf_sign_placement_templates.json")

    fun listTemplates(): List<SignaturePlacementTemplateMeta> {
        return readTemplates()
            .sortedByDescending { template -> template.updatedAtMillis }
            .map { template ->
                SignaturePlacementTemplateMeta(
                    name = template.name,
                    placementCount = template.placements.size,
                    updatedAtMillis = template.updatedAtMillis
                )
            }
    }

    fun loadTemplate(name: String): List<PdfSignaturePlacementUi>? {
        val normalized = normalizeName(name)
        val template = readTemplates().firstOrNull { item ->
            item.name.equals(normalized, ignoreCase = true)
        } ?: return null
        return template.placements
    }

    fun saveTemplate(name: String, placements: List<PdfSignaturePlacementUi>) = synchronized(TEMPLATE_LOCK) {
        val normalized = normalizeName(name)
        require(placements.isNotEmpty()) { "Template placements cannot be empty." }
        require(placements.size <= MAX_TEMPLATE_PLACEMENTS) {
            "Template supports at most $MAX_TEMPLATE_PLACEMENTS placements."
        }

        val existing = readTemplates().toMutableList()
        val now = System.currentTimeMillis()
        val updated = TemplateRecord(
            name = normalized,
            updatedAtMillis = now,
            placements = placements.map { placement ->
                placement.copy(
                    pageOneBased = placement.pageOneBased.coerceAtLeast(1),
                    xRatio = placement.xRatio.coerceIn(0f, 1f),
                    yRatio = placement.yRatio.coerceIn(0f, 1f),
                    widthRatio = placement.widthRatio.coerceIn(0.1f, 0.8f)
                )
            }
        )

        val existingIndex = existing.indexOfFirst { item ->
            item.name.equals(normalized, ignoreCase = true)
        }
        if (existingIndex >= 0) {
            existing[existingIndex] = updated
        } else {
            require(existing.size < MAX_TEMPLATES) { "Template limit reached ($MAX_TEMPLATES)." }
            existing += updated
        }
        writeTemplates(existing)
    }

    fun deleteTemplate(name: String) = synchronized(TEMPLATE_LOCK) {
        val normalized = normalizeName(name)
        val remaining = readTemplates().filterNot { template ->
            template.name.equals(normalized, ignoreCase = true)
        }
        writeTemplates(remaining)
    }

    private fun readTemplates(): List<TemplateRecord> {
        if (!templatesFile.exists()) return emptyList()
        try {
            val root = JSONObject(templatesFile.readText())
            val templates = root.getJSONArray("templates")
            require(templates.length() <= MAX_TEMPLATES) { "Too many stored templates." }
            val output = mutableListOf<TemplateRecord>()
            for (index in 0 until templates.length()) {
                val obj = templates.getJSONObject(index)
                val rawName = obj.get("name")
                require(rawName is String) { "Invalid stored template name." }
                val name = rawName.trim()
                require(name.isNotEmpty() && name.length <= MAX_NAME_LENGTH) {
                    "Invalid stored template name."
                }
                require(output.none { it.name.equals(name, ignoreCase = true) }) {
                    "Duplicate stored template name."
                }
                val rawTimestamp = obj.get("updatedAtMillis")
                require(rawTimestamp is Number) { "Invalid stored template timestamp." }
                val updatedAt = rawTimestamp.toLong().coerceAtLeast(0L)
                val placementsArray = obj.getJSONArray("placements")
                require(placementsArray.length() in 1..MAX_TEMPLATE_PLACEMENTS) {
                    "Invalid stored template placements."
                }
                val placements = mutableListOf<PdfSignaturePlacementUi>()
                for (placementIndex in 0 until placementsArray.length()) {
                    val placementObj = placementsArray.getJSONObject(placementIndex)
                    val page = placementObj.getInt("pageOneBased")
                    val x = placementObj.getDouble("xRatio").toFloat()
                    val y = placementObj.getDouble("yRatio").toFloat()
                    val width = placementObj.getDouble("widthRatio").toFloat()
                    require(page > 0 && x.isFinite() && y.isFinite() && width.isFinite()) {
                        "Invalid stored signature placement."
                    }
                    placements += PdfSignaturePlacementUi(
                        pageOneBased = page,
                        xRatio = x.coerceIn(0f, 1f),
                        yRatio = y.coerceIn(0f, 1f),
                        widthRatio = width.coerceIn(0.1f, 0.8f)
                    )
                }
                output += TemplateRecord(name, updatedAt, placements)
            }
            return output
        } catch (failure: Exception) {
            throw java.io.IOException("Stored signature templates are unreadable or corrupt.", failure)
        }
    }

    private fun writeTemplates(templates: List<TemplateRecord>) {
        val root = JSONObject()
        val array = JSONArray()
        templates.forEach { template ->
            val templateObj = JSONObject()
                .put("name", template.name)
                .put("updatedAtMillis", template.updatedAtMillis)
            val placementsArray = JSONArray()
            template.placements.forEach { placement ->
                placementsArray.put(
                    JSONObject()
                        .put("pageOneBased", placement.pageOneBased)
                        .put("xRatio", placement.xRatio.toDouble())
                        .put("yRatio", placement.yRatio.toDouble())
                        .put("widthRatio", placement.widthRatio.toDouble())
                )
            }
            templateObj.put("placements", placementsArray)
            array.put(templateObj)
        }
        root.put("templates", array)

        writeJsonAtomically(templatesFile, root.toString())
    }

    private fun normalizeName(raw: String): String {
        val cleaned = raw.trim()
        require(cleaned.isNotBlank()) { "Template name is required." }
        require(cleaned.length <= MAX_NAME_LENGTH) {
            "Template name must be <= $MAX_NAME_LENGTH characters."
        }
        return cleaned
    }

    private data class TemplateRecord(
        val name: String,
        val updatedAtMillis: Long,
        val placements: List<PdfSignaturePlacementUi>
    )

    companion object {
        private val TEMPLATE_LOCK = Any()

        const val MAX_TEMPLATES: Int = 20
        const val MAX_TEMPLATE_PLACEMENTS: Int = 120
        const val MAX_NAME_LENGTH: Int = 40
    }
}
