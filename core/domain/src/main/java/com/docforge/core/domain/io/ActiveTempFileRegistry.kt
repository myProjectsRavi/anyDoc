package com.docforge.core.domain.io

import java.io.File
import java.util.ArrayDeque
import java.nio.file.Files
import java.nio.file.Path

/**
 * Process-local protection for temporary files currently used by conversions.
 *
 * Registration and deletion share one lock. Canonical identities protect
 * equivalent paths, while the registration ledger retains the original identity
 * even if a symlink is retargeted before release.
 */
object ActiveTempFileRegistry {
    private val lock = Any()
    private val activePaths = HashMap<String, Int>()
    private val registeredKeysByPath = HashMap<String, ArrayDeque<String>>()
    private val deferredReleasesByPath = HashMap<String, ArrayDeque<String>>()

    /**
     * Resolve each path component before processing the next one. This handles
     * dangling parent symlinks and preserves filesystem semantics for `link/..`.
     * File.canonicalPath alone does not resolve dangling symlink chains.
     */
    private fun identity(file: File): String {
        val absolute = file.absoluteFile.toPath()
        val pending = ArrayDeque<String>()
        for (component in absolute) pending.addLast(component.toString())
        var resolved: Path = absolute.root ?: throw java.io.IOException("Expected absolute temporary path")
        var followedLinks = 0
        while (pending.isNotEmpty()) {
            when (val component = pending.removeFirst()) {
                "", "." -> Unit
                ".." -> resolved = resolved.parent ?: resolved
                else -> {
                    val candidate = resolved.resolve(component)
                    if (Files.isSymbolicLink(candidate)) {
                        if (++followedLinks > 32) {
                            throw java.io.IOException("Too many symbolic links in temporary file path")
                        }
                        val target = Files.readSymbolicLink(candidate)
                        if (target.isAbsolute) resolved = target.root
                        for (part in target.toList().asReversed()) {
                            pending.addFirst(part.toString())
                        }
                    } else {
                        resolved = candidate
                    }
                }
            }
        }
        return resolved.toFile().canonicalPath
    }

    fun register(file: File) {
        synchronized(lock) {
            val key = identity(file)
            val owners = activePaths[key] ?: 0
            check(owners < Int.MAX_VALUE) { "Temporary file owner count overflow" }
            registeredKeysByPath.getOrPut(file.absolutePath) { ArrayDeque() }.addLast(key)
            activePaths[key] = owners + 1
        }
    }

    fun unregister(file: File) {
        synchronized(lock) {
            val path = file.absolutePath
            // Unregistration must match the original lexical registration path.
            // Resolving an unregistered alias would allow an unrelated caller to
            // release another owner's protection without owning a registration.
            val remaining = registeredKeysByPath[path] ?: return
            if (remaining.isEmpty()) return
            val key = remaining.removeLast()
            val deferred = deferredReleasesByPath[path]
            if (deferred != null || remaining.any { it != key }) {
                // A retargeted alias can represent multiple live owners. Since
                // unregister has no ownership token, release order is unknown.
                // Retain *all* identities until this alias's final release.
                deferredReleasesByPath.getOrPut(path) { ArrayDeque() }.addLast(key)
                if (remaining.isEmpty()) {
                    registeredKeysByPath.remove(path)
                    deferredReleasesByPath.remove(path)?.forEach(::releaseKey)
                }
            } else {
                if (remaining.isEmpty()) registeredKeysByPath.remove(path)
                releaseKey(key)
            }
        }
    }

    private fun releaseKey(key: String) {
        val owners = activePaths[key] ?: return
        if (owners == 1) activePaths.remove(key) else activePaths[key] = owners - 1
    }

    fun isActive(file: File): Boolean = synchronized(lock) {
        activePaths.containsKey(identity(file))
    }

    /** An active check and physical deletion cannot race registration. */
    fun deleteIfInactive(file: File): Boolean = synchronized(lock) {
        if (activePaths.containsKey(identity(file))) false else file.delete()
    }
}
