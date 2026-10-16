package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class PdfStagedRollbackIdentityTest {
    private fun fileKey(file: File): Any? = Files.readAttributes(
        file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS
    ).fileKey()

    @Test fun unchangedPublishedEntryIsRemoved() {
        val root = Files.createTempDirectory("rollback-unchanged").toFile()
        try {
            val output = File(root, "report.pdf").apply { writeText("ours") }
            val identity = fileKey(output)
            assumeTrue(identity != null)
            assertTrue(rollbackPublishedOutputIfUnchanged(output, identity))
            assertFalse(output.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun replacedRegularEntryIsPreserved() {
        val root = Files.createTempDirectory("rollback-replaced").toFile()
        try {
            val output = File(root, "report.pdf").apply { writeText("ours") }
            val identity = fileKey(output)
            assumeTrue(identity != null)
            val replacement = File(root, "replacement.pdf").apply { writeText("keep") }
            assumeTrue(identity != fileKey(replacement))
            Files.move(replacement.toPath(), output.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            assertFalse(rollbackPublishedOutputIfUnchanged(output, identity))
            assertEquals("keep", output.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun replacedSymbolicLinkIsNotUnlinkedOrFollowed() {
        val root = Files.createTempDirectory("rollback-symlink").toFile()
        try {
            val output = File(root, "report.pdf").apply { writeText("ours") }
            val identity = fileKey(output)
            assumeTrue(identity != null)
            val external = File(root, "external.txt").apply { writeText("keep") }
            assertTrue(output.delete())
            Files.createSymbolicLink(output.toPath(), external.toPath())
            assertFalse(rollbackPublishedOutputIfUnchanged(output, identity))
            assertTrue(Files.isSymbolicLink(output.toPath()))
            assertEquals("keep", external.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun substitutedDirectoryIsNotDeleted() {
        val root = Files.createTempDirectory("rollback-directory").toFile()
        try {
            val output = File(root, "report.pdf").apply { writeText("ours") }
            val identity = fileKey(output)
            assumeTrue(identity != null)
            assertTrue(output.delete())
            assertTrue(output.mkdir())
            File(output, "keep.txt").writeText("keep")
            assertFalse(rollbackPublishedOutputIfUnchanged(output, identity))
            assertEquals("keep", File(output, "keep.txt").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun unknownIdentityNeverDeletesOutput() {
        val root = Files.createTempDirectory("rollback-unknown").toFile()
        try {
            val output = File(root, "report.pdf").apply { writeText("keep") }
            assertFalse(rollbackPublishedOutputIfUnchanged(output, null))
            assertEquals("keep", output.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun alreadyMissingPublishedEntryIsHarmless() {
        val root = Files.createTempDirectory("rollback-missing").toFile()
        try {
            val output = File(root, "report.pdf").apply { writeText("ours") }
            val identity = fileKey(output)
            assumeTrue(identity != null)
            assertTrue(output.delete())
            assertFalse(rollbackPublishedOutputIfUnchanged(output, identity))
        } finally {
            root.deleteRecursively()
        }
    }
}
