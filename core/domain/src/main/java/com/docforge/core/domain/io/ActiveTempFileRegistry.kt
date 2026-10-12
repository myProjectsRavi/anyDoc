package com.docforge.core.domain.io

import java.io.File

/**
 * Process-local registry for temporary files currently used by conversions.
 *
 * Registration and cleanup deletion share one lock. Cleanup must not delete a
 * file after its owner has registered it but before the owner unregisters it.
 * Registration after deletion cannot resurrect the file; callers must register
 * before starting work with a temporary file.
 */
object ActiveTempFileRegistry {
    private val lock = Any()
    private val activePaths = HashSet<String>()

    fun register(file: File) {
        synchronized(lock) {
            activePaths.add(file.absolutePath)
        }
    }

    fun unregister(file: File) {
        synchronized(lock) {
            activePaths.remove(file.absolutePath)
        }
    }

    fun isActive(file: File): Boolean = synchronized(lock) {
        file.absolutePath in activePaths
    }

    /**
     * One critical section covers the active check and physical deletion.
     * A successful registration cannot interleave between them.
     */
    fun deleteIfInactive(file: File): Boolean = synchronized(lock) {
        if (file.absolutePath in activePaths) false else file.delete()
    }
}
