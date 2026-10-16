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

class PdfStagedPublicationSwapTest {
    private fun reject(block: () -> Unit) {
        try {
            block()
            fail("Expected substituted staging entry to be rejected")
        } catch (_: IllegalArgumentException) {
            // The moved entry must have been removed before surfacing the failure.
        }
    }

    @Test fun symlinkSwappedAfterValidationIsNotLeftPublished() {
        val root = Files.createTempDirectory("anydoc-stage-swap").toFile()
        val stage = File(root, ".anydoc_stage_swap.part")
        val final = File(root, "report.pdf")
        val outside = File(root, "external.txt").apply { writeText("keep") }
        try {
            stage.writeText("safe")
            reject {
                publishCheckedStagedOutput(stage, final) {
                    assertTrue(stage.delete())
                    Files.createSymbolicLink(stage.toPath(), outside.toPath())
                }
            }
            assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
            assertFalse(Files.exists(stage.toPath(), LinkOption.NOFOLLOW_LINKS))
            assertEquals("keep", outside.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun danglingSymlinkSwapDoesNotCreateExternalFile() {
        val root = Files.createTempDirectory("anydoc-stage-dangling-swap").toFile()
        val stage = File(root, ".anydoc_stage_swap.part")
        val final = File(root, "report.pdf")
        val outside = File(root, "missing.pdf")
        try {
            stage.writeText("safe")
            reject {
                publishCheckedStagedOutput(stage, final) {
                    assertTrue(stage.delete())
                    Files.createSymbolicLink(stage.toPath(), outside.toPath())
                }
            }
            assertFalse(outside.exists())
            assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
            assertFalse(Files.exists(stage.toPath(), LinkOption.NOFOLLOW_LINKS))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun swappedRegularFileWithDifferentIdentityIsRejectedWhenSupported() {
        val root = Files.createTempDirectory("anydoc-regular-swap").toFile()
        val stage = File(root, ".anydoc_stage_swap.part")
        val final = File(root, "report.pdf")
        val replacement = File(root, "replacement.pdf")
        try {
            stage.writeText("original")
            replacement.writeText("substituted")
            val originalKey = Files.readAttributes(
                stage.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS
            ).fileKey()
            val replacementKey = Files.readAttributes(
                replacement.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS
            ).fileKey()
            assumeTrue(originalKey != null && replacementKey != null && originalKey != replacementKey)
            reject {
                publishCheckedStagedOutput(stage, final) {
                    assertTrue(stage.delete())
                    Files.move(replacement.toPath(), stage.toPath())
                }
            }
            assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun substitutedNonemptyDirectoryIsRemovedWithoutLeavingPublishedPath() {
        val root = Files.createTempDirectory("anydoc-stage-dir-swap").toFile()
        val stage = File(root, ".anydoc_stage_swap.part")
        val final = File(root, "report.pdf")
        val external = File(root, "external.txt").apply { writeText("keep") }
        try {
            stage.writeText("safe")
            reject {
                publishCheckedStagedOutput(stage, final) {
                    assertTrue(stage.delete())
                    assertTrue(stage.mkdir())
                    File(stage, "payload.txt").writeText("discard")
                    Files.createSymbolicLink(File(stage, "link").toPath(), external.toPath())
                }
            }
            assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
            assertEquals("keep", external.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun normalRegularStageStillPublishesWithoutOverwrite() {
        val root = Files.createTempDirectory("anydoc-stage-normal").toFile()
        val stage = File(root, ".anydoc_stage_normal.part")
        val final = File(root, "report.pdf")
        try {
            stage.writeText("finished")
            publishCheckedStagedOutput(stage, final)
            assertEquals("finished", final.readText())
            assertFalse(Files.exists(stage.toPath(), LinkOption.NOFOLLOW_LINKS))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun alreadyOccupiedDestinationRemainsUnchanged() {
        val root = Files.createTempDirectory("anydoc-stage-occupied").toFile()
        val stage = File(root, ".anydoc_stage_occupied.part")
        val final = File(root, "report.pdf")
        try {
            stage.writeText("new")
            final.writeText("existing")
            try {
                publishCheckedStagedOutput(stage, final)
                fail("Expected occupied destination to be rejected")
            } catch (_: java.nio.file.FileAlreadyExistsException) {
                assertEquals("existing", final.readText())
                assertEquals("new", stage.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
