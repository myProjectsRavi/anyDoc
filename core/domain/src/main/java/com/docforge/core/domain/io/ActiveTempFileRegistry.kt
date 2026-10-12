package com.docforge.core.domain.io

import java.io.File

/**
 * Process-local registry for temporary files currently used by conversions.
 *
 * Registration and cleanup deletion share one lock. Cleanup must not delete a
 * file after its owner has registered it but before the owner unregisters it.
 * Multiple owners of the same path retain protection until the last unregisters.
 * Registration after deletion cannot resurrect the file; callers must register
 * before starting work with a temporary file.
 */
object ActiveTempFileRegistry {
    private val lock = Any()
    private val activePaths = HashMap<String, Int>()

    fun register(file: File) {
        synchronized(lock) {
            val path = file.absolutePath
            val owners = activePaths[path] ?: 0
            check(owners < Int.MAX_VALUE) { "Temporary file owner count overflow" }
            activePaths[path] = owners + 1
        }
    }

    fun unregister(file: File) {
        synchronized(lock) {
            val path = file.absolutePath
            val owners = activePaths[path] ?: return
            if (owners == 1) activePaths.remove(path) else activePaths[path] = owners - 1
        }
    }

    fun isActive(file: File): Boolean = synchronized(lock) {
        activePaths.containsKey(file.absolutePath)
    }

    /**
     * One critical section covers the active check and physical deletion.
     * A successful registration cannot interleave between them.
     */
    fun deleteIfInactive(file: File): Boolean = synchronized(lock) {
        if (activePaths.containsKey(file.absolutePath)) false else file.delete()
    }
}
