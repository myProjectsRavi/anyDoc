package com.docforge.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val onboardingCompleted: Boolean,
    val defaultPdfPageSizeName: String,
    val defaultPdfCompressionName: String,
    val defaultImageQuality: Int,
    val documentsFolderName: String,
    val imagesFolderName: String,
    val audioFolderName: String
)

class AppSettingsRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        DocForgeSettingsStore.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = readSettings()
    }

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun currentSettings(): AppSettings = _settings.value

    fun isOnboardingCompleted(): Boolean = DocForgeSettingsStore.readOnboardingCompleted(appContext)

    fun markOnboardingCompleted() {
        DocForgeSettingsStore.writeOnboardingCompleted(appContext, true)
    }

    fun setDefaultPdfPageSizeName(name: String) {
        DocForgeSettingsStore.writePdfPageSizeName(appContext, name)
    }

    fun setDefaultPdfCompressionName(name: String) {
        DocForgeSettingsStore.writePdfCompressionName(appContext, name)
    }

    fun setDefaultImageQuality(value: Int) {
        DocForgeSettingsStore.writeDefaultImageQuality(appContext, value)
    }

    fun setDocumentsFolderName(name: String) {
        DocForgeSettingsStore.writeOutputFolderName(appContext, DocForgeOutputBucket.DOCUMENTS, name)
    }

    fun setImagesFolderName(name: String) {
        DocForgeSettingsStore.writeOutputFolderName(appContext, DocForgeOutputBucket.PICTURES, name)
    }

    fun setAudioFolderName(name: String) {
        DocForgeSettingsStore.writeOutputFolderName(appContext, DocForgeOutputBucket.AUDIO, name)
    }

    private fun readSettings(): AppSettings {
        return AppSettings(
            onboardingCompleted = DocForgeSettingsStore.readOnboardingCompleted(appContext),
            defaultPdfPageSizeName = DocForgeSettingsStore.readPdfPageSizeName(appContext),
            defaultPdfCompressionName = DocForgeSettingsStore.readPdfCompressionName(appContext),
            defaultImageQuality = DocForgeSettingsStore.readDefaultImageQuality(appContext),
            documentsFolderName = DocForgeSettingsStore.readOutputFolderName(appContext, DocForgeOutputBucket.DOCUMENTS),
            imagesFolderName = DocForgeSettingsStore.readOutputFolderName(appContext, DocForgeOutputBucket.PICTURES),
            audioFolderName = DocForgeSettingsStore.readOutputFolderName(appContext, DocForgeOutputBucket.AUDIO)
        )
    }
}
