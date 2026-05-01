package com.docforge.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.StrictMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class DocForgeApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        scheduleCacheCleanup()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                appScope.launch {
                    cleanStaleTempFiles(maxAgeMs = 0L)
                }
            }
        }
    }

    private fun scheduleCacheCleanup() {
        appScope.launch {
            cleanStaleTempFiles(maxAgeMs = TEMP_FILE_MAX_AGE_MS)
        }
    }

    private fun cleanStaleTempFiles(maxAgeMs: Long) {
        val now = System.currentTimeMillis()
        val cutoff = now - maxAgeMs
        cacheDir.listFiles()
            ?.filter { file -> file.name.startsWith(TEMP_FILE_PREFIX) && file.lastModified() < cutoff }
            ?.forEach { file -> runCatching { file.delete() } }

        val externalCache = externalCacheDir
        externalCache?.listFiles()
            ?.filter { file -> file.name.startsWith(TEMP_FILE_PREFIX) && file.lastModified() < cutoff }
            ?.forEach { file -> runCatching { file.delete() } }
    }

    companion object {
        private const val TEMP_FILE_PREFIX = "docforge_"
        private const val TEMP_FILE_MAX_AGE_MS = 30 * 60 * 1000L // 30 minutes
    }
}
