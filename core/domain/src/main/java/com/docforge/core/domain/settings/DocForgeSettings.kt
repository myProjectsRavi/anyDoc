package com.docforge.core.domain.settings

import android.content.Context
import android.os.Environment
import java.io.File

enum class DocForgeOutputBucket(val mediaDirectory: String) {
    DOCUMENTS(Environment.DIRECTORY_DOCUMENTS),
    PICTURES(Environment.DIRECTORY_PICTURES),
    AUDIO(Environment.DIRECTORY_MUSIC)
}

object DocForgeSettingsStore {
    const val PREFS_NAME = "docforge_settings"
    const val DEFAULT_OUTPUT_FOLDER = "DocForge"
    private val VALID_FOLDER_REGEX = Regex("[^a-zA-Z0-9 _-]")

    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    const val KEY_DEFAULT_PDF_PAGE_SIZE = "default_pdf_page_size"
    const val KEY_DEFAULT_PDF_COMPRESSION = "default_pdf_compression"
    const val KEY_DEFAULT_IMAGE_QUALITY = "default_image_quality"
    const val KEY_DOCUMENTS_FOLDER_NAME = "documents_folder_name"
    const val KEY_IMAGES_FOLDER_NAME = "images_folder_name"
    const val KEY_AUDIO_FOLDER_NAME = "audio_folder_name"

    fun readOnboardingCompleted(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun writeOnboardingCompleted(context: Context, completed: Boolean) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun readPdfPageSizeName(context: Context): String {
        return prefs(context).getString(KEY_DEFAULT_PDF_PAGE_SIZE, "A4").orEmpty().ifBlank { "A4" }
    }

    fun writePdfPageSizeName(context: Context, value: String) {
        prefs(context).edit().putString(KEY_DEFAULT_PDF_PAGE_SIZE, value).apply()
    }

    fun readPdfCompressionName(context: Context): String {
        return prefs(context).getString(KEY_DEFAULT_PDF_COMPRESSION, "MEDIUM").orEmpty().ifBlank { "MEDIUM" }
    }

    fun writePdfCompressionName(context: Context, value: String) {
        prefs(context).edit().putString(KEY_DEFAULT_PDF_COMPRESSION, value).apply()
    }

    fun readDefaultImageQuality(context: Context): Int {
        val raw = prefs(context).getInt(KEY_DEFAULT_IMAGE_QUALITY, 90)
        return raw.coerceIn(10, 100)
    }

    fun writeDefaultImageQuality(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_DEFAULT_IMAGE_QUALITY, value.coerceIn(10, 100)).apply()
    }

    fun readOutputFolderName(context: Context, bucket: DocForgeOutputBucket): String {
        val key = outputFolderKey(bucket)
        val stored = prefs(context).getString(key, DEFAULT_OUTPUT_FOLDER).orEmpty()
        return sanitizeFolderName(stored)
    }

    fun writeOutputFolderName(context: Context, bucket: DocForgeOutputBucket, value: String) {
        val key = outputFolderKey(bucket)
        prefs(context).edit().putString(key, sanitizeFolderName(value)).apply()
    }

    fun resolveOutputDirectory(context: Context, bucket: DocForgeOutputBucket): File {
        val baseDir = context.getExternalFilesDir(bucket.mediaDirectory) ?: context.filesDir
        val folderName = readOutputFolderName(context, bucket)
        return File(baseDir, folderName).apply { mkdirs() }
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun outputFolderKey(bucket: DocForgeOutputBucket): String {
        return when (bucket) {
            DocForgeOutputBucket.DOCUMENTS -> KEY_DOCUMENTS_FOLDER_NAME
            DocForgeOutputBucket.PICTURES -> KEY_IMAGES_FOLDER_NAME
            DocForgeOutputBucket.AUDIO -> KEY_AUDIO_FOLDER_NAME
        }
    }

    private fun sanitizeFolderName(raw: String): String {
        val cleaned = raw.trim().replace(VALID_FOLDER_REGEX, "_")
        return if (cleaned.isBlank()) DEFAULT_OUTPUT_FOLDER else cleaned
    }
}
