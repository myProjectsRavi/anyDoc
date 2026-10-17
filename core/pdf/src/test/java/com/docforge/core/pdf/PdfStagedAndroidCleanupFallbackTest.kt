package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Host tests simulate Android identity when NIO fileKey is unavailable. */
class PdfStagedAndroidCleanupFallbackTest {
    private fun key(file: File): Any? =
        Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).fileKey()

    private fun reader(): (File) -> StagedAndroidIdentity? {
        val known = mutableMapOf<Any, Long>()
        return { file ->
            val k = key(file)
            if (k == null) null else StagedAndroidIdentity(7L, known.getOrPut(k) { known.size.toLong() + 1L })
        }
    }

    @Test fun ownedStageIsDeletedWhenNioIdentityUnavailable() {
        val root = Files.createTempDirectory("stage-fallback-owned").toFile()
        try {
            val stage = File(root, "stage.part").apply { writeText("ours") }
            val read = reader()
            val original = read(stage)
            assumeTrue(original != null)
            assertTrue(deleteOwnedStagedEntry(stage, null, original, read))
            assertFalse(stage.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun ownedPublishedOutputIsRolledBackWhenNioIdentityUnavailable() {
        val root = Files.createTempDirectory("rollback-fallback-owned").toFile()
        try {
            val output = File(root, "out.pdf").apply { writeText("ours") }
            val read = reader()
            val original = read(output)
            assumeTrue(original != null)
            assertTrue(rollbackPublishedOutputIfUnchanged(output, null, original, read))
            assertFalse(output.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun substitutedStageAndOutputArePreserved() {
        val root = Files.createTempDirectory("fallback-replacement").toFile()
        try {
            val stage = File(root, "stage.part").apply { writeText("ours") }
            val output = File(root, "out.pdf").apply { writeText("ours") }
            val foreignStage = File(root, "other-stage").apply { writeText("keep stage") }
            val foreignOutput = File(root, "other-output").apply { writeText("keep output") }
            val read = reader()
            val stageIdentity = read(stage)
            val outputIdentity = read(output)
            assumeTrue(stageIdentity != null && outputIdentity != null)
            assumeTrue(key(stage) != key(foreignStage) && key(output) != key(foreignOutput))
            Files.move(foreignStage.toPath(), stage.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.move(foreignOutput.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
            assertFalse(deleteOwnedStagedEntry(stage, null, stageIdentity, read))
            assertFalse(rollbackPublishedOutputIfUnchanged(output, null, outputIdentity, read))
            assertEquals("keep stage", stage.readText())
            assertEquals("keep output", output.readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun symlinkAndDirectoryAreNeverDeleted() {
        val root = Files.createTempDirectory("fallback-types").toFile()
        try {
            val stage = File(root, "stage.part").apply { writeText("ours") }
            val output = File(root, "out.pdf").apply { writeText("ours") }
            val external = File(root, "external").apply { writeText("keep") }
            val read = reader()
            val stageIdentity = read(stage)
            val outputIdentity = read(output)
            assumeTrue(stageIdentity != null && outputIdentity != null)
            assertTrue(stage.delete())
            Files.createSymbolicLink(stage.toPath(), external.toPath())
            assertTrue(output.delete())
            assertTrue(output.mkdir())
            File(output, "nested").writeText("keep")
            assertFalse(deleteOwnedStagedEntry(stage, null, stageIdentity, read))
            assertFalse(rollbackPublishedOutputIfUnchanged(output, null, outputIdentity, read))
            assertTrue(Files.isSymbolicLink(stage.toPath()))
            assertEquals("keep", external.readText())
            assertEquals("keep", File(output, "nested").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun unknownAndMissingIdentitiesFailClosed() {
        val root = Files.createTempDirectory("fallback-unknown").toFile()
        try {
            val stage = File(root, "stage.part").apply { writeText("keep") }
            val output = File(root, "out.pdf").apply { writeText("keep") }
            val known = StagedAndroidIdentity(7L, 100L)
            assertFalse(deleteOwnedStagedEntry(stage, null, null) { known })
            assertFalse(deleteOwnedStagedEntry(stage, null, known) { null })
            assertFalse(rollbackPublishedOutputIfUnchanged(output, null, null) { known })
            assertFalse(rollbackPublishedOutputIfUnchanged(output, null, known) { null })
            assertTrue(stage.exists())
            assertTrue(output.exists())
            assertTrue(stage.delete())
            assertTrue(output.delete())
            assertFalse(deleteOwnedStagedEntry(stage, null, known) { known })
            assertFalse(rollbackPublishedOutputIfUnchanged(output, null, known) { known })
        } finally { root.deleteRecursively() }
    }

    @Test fun nioIdentityTakesPrecedenceOverAndroidIdentity() {
        val root = Files.createTempDirectory("fallback-priority").toFile()
        try {
            val stage = File(root, "stage.part").apply { writeText("keep") }
            val output = File(root, "out.pdf").apply { writeText("keep") }
            val android = StagedAndroidIdentity(7L, 100L)
            assertFalse(deleteOwnedStagedEntry(stage, "wrong-key", android) { android })
            assertFalse(rollbackPublishedOutputIfUnchanged(output, "wrong-key", android) { android })
            assertTrue(stage.exists())
            assertTrue(output.exists())
        } finally { root.deleteRecursively() }
    }
}
