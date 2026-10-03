package com.docforge.core.domain.io

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-local registry for temporary files that are actively in use.
 *
 * Cache cleanup may delete stale AnyDoc temporary files, but it must never delete a file while
 * an operation is still reading or writing it. Entries intentionally live only for the current
 * process: after process death there can be no in-flight operation, so leftovers are safe for the
 * next startup cleanup to reclaim.
 */
object ActiveTempFileRegistry {
    private val activePaths = ConcurrentHashMap.newKeySet<String>()

    fun register(file: File) {
        activePaths += file.absolutePath
    }

    fun unregister(file: File) {
        activePaths -= file.absolutePath
    }

    fun isActive(file: File): Boolean = file.absolutePath in activePaths
}
