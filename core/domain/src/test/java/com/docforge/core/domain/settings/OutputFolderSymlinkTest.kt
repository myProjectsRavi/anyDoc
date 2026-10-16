package com.docforge.core.domain.settings

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OutputFolderSymlinkTest {
    private fun rejects(block: () -> Unit) {
        try {
            block()
            fail("Expected symlinked output folder rejection")
        } catch (_: IllegalArgumentException) {
            // Expected: output path must not be redirected.
        }
    }

    @Test fun existingSymlinkedFolderIsRejected() {
        val root = Files.createTempDirectory("anydoc-domain-linked").toFile()
        try {
            val external = File(root, "external").apply { mkdirs() }
            val folder = File(root, "AnyDoc")
            Files.createSymbolicLink(folder.toPath(), external.toPath())
            rejects { DocForgeSettingsStore.ensureUnlinkedOutputFolder(folder) }
            assertEquals(emptyList<String>(), external.listFiles().orEmpty().map { it.name })
        } finally { root.deleteRecursively() }
    }

    @Test fun danglingSymlinkIsRejected() {
        val root = Files.createTempDirectory("anydoc-domain-dangling").toFile()
        try {
            val target = File(root, "absent")
            val folder = File(root, "AnyDoc")
            Files.createSymbolicLink(folder.toPath(), target.toPath())
            assertFalse(folder.exists())
            rejects { DocForgeSettingsStore.ensureUnlinkedOutputFolder(folder) }
            assertFalse(target.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun regularFolderAndExistingDirectoryWork() {
        val root = Files.createTempDirectory("anydoc-domain-normal").toFile()
        try {
            val folder = File(root, "AnyDoc")
            assertTrue(DocForgeSettingsStore.ensureUnlinkedOutputFolder(folder))
            assertTrue(folder.isDirectory)
            assertTrue(DocForgeSettingsStore.ensureUnlinkedOutputFolder(folder))
        } finally { root.deleteRecursively() }
    }

    @Test fun ordinaryFileIsNotAcceptedAsDirectory() {
        val root = Files.createTempDirectory("anydoc-domain-file").toFile()
        try {
            val folder = File(root, "AnyDoc").apply { writeText("existing") }
            assertFalse(DocForgeSettingsStore.ensureUnlinkedOutputFolder(folder))
            assertEquals("existing", folder.readText())
        } finally { root.deleteRecursively() }
    }
}
