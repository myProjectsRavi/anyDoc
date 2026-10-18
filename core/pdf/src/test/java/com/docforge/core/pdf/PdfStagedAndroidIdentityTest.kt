package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class PdfStagedAndroidIdentityTest {
    private fun key(file: File): Any? =
        Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).fileKey()

    private fun simulatedAndroidReader(): (File) -> StagedAndroidIdentity? {
        val identities = mutableMapOf<Any, Long>()
        return { file ->
            val nioKey = key(file)
            if (nioKey == null) null else {
                val inode = identities.getOrPut(nioKey) { identities.size.toLong() + 1L }
                StagedAndroidIdentity(7L, inode)
            }
        }
    }

    @Test fun knownNioIdentityHasPriorityOverAndroidFallback() {
        assertEquals(true, stagedIdentityContinuity("original", StagedAndroidIdentity(1L, 2L), "original", null))
        assertEquals(false, stagedIdentityContinuity("original", StagedAndroidIdentity(1L, 2L), "other", StagedAndroidIdentity(1L, 2L)))
        assertEquals(false, stagedIdentityContinuity("original", null, null, null))
    }

    @Test fun missingNioKeyUsesKnownAndroidDeviceAndInode() {
        val original = StagedAndroidIdentity(5L, 101L)
        assertEquals(true, stagedIdentityContinuity(null, original, null, original))
        assertEquals(false, stagedIdentityContinuity(null, original, null, StagedAndroidIdentity(5L, 102L)))
        assertEquals(false, stagedIdentityContinuity(null, original, null, StagedAndroidIdentity(6L, 101L)))
        assertEquals(false, stagedIdentityContinuity(null, original, null, null))
        assertEquals(null, stagedIdentityContinuity(null, null, null, null))
    }

    @Test fun fallbackIdentityAllowsNormalPublication() {
        val root = Files.createTempDirectory("anydoc-android-identity-normal").toFile()
        try {
            val stage = File(root, ".anydoc_stage_test.part").apply { writeText("owned") }
            val output = File(root, "result.pdf")
            val reader = simulatedAndroidReader()
            val original = reader(stage)
            assumeTrue(original != null)
            publishCheckedStagedOutput(stage, output, null, original, reader)
            assertEquals("owned", output.readText())
            assertFalse(stage.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun fallbackIdentityRejectsPreMoveRegularFileSubstitution() {
        val root = Files.createTempDirectory("anydoc-android-identity-swap").toFile()
        try {
            val stage = File(root, ".anydoc_stage_test.part").apply { writeText("owned") }
            val foreign = File(root, "foreign.txt").apply { writeText("foreign") }
            val output = File(root, "result.pdf")
            val reader = simulatedAndroidReader()
            val original = reader(stage)
            assumeTrue(original != null && key(stage) != key(foreign))
            try {
                publishCheckedStagedOutput(stage, output, null, original, reader) {
                    assertTrue(stage.delete())
                    Files.move(foreign.toPath(), stage.toPath())
                }
                fail("Expected fallback identity mismatch")
            } catch (_: IllegalArgumentException) {
                assertFalse(Files.exists(output.toPath(), LinkOption.NOFOLLOW_LINKS))
                assertEquals("foreign", stage.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
