package com.docforge.app

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File

/**
 * Reclaims only old, inactive AnyDoc temporary files.
 *
 * A memory-trim callback must not delete newly created cache files that have
 * not yet been registered as active by their owner.
 */
internal object DocForgeTempFileCleanup {
    fun clean(directory: File?, nowMillis: Long, minimumAgeMillis: Long) {
        require(minimumAgeMillis >= 0L) { "Cleanup age must not be negative." }
        directory?.listFiles()?.forEach { file ->
            if (!file.isFile || !file.name.startsWith("docforge_")) return@forEach
            val modifiedAt = file.lastModified()
            if (modifiedAt <= 0L || modifiedAt > nowMillis) return@forEach
            if (nowMillis - modifiedAt < minimumAgeMillis) return@forEach
            if (ActiveTempFileRegistry.isActive(file)) return@forEach
            runCatching { file.delete() }
        }
    }
}
