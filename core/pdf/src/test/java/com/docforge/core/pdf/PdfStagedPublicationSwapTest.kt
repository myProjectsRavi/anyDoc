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
            // The original publication failure must be surfaced without destroying foreign entries.
        }
    }

    @Test fun symlinkSwappedAfterValidationPreservesForeignLink() {
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
            assertTrue(Files.isSymbolicLink(final.toPath()))
            assertEquals(outside.toPath(), Files.readSymbolicLink(final.toPath()))
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
            assertTrue(Files.isSymbolicLink(final.toPath()))
            assertEquals(outside.toPath(), Files.readSymbolicLink(final.toPath()))
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
            assertEquals("substituted", final.readText())
            assertFalse(Files.exists(stage.toPath(), LinkOption.NOFOLLOW_LINKS))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun substitutedNonemptyDirectoryPreservesForeignContents() {
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
                    File(stage, "payload.txt").writeText("preserve")
                    Files.createSymbolicLink(File(stage, "link").toPath(), external.toPath())
                }
            }
            assertTrue(Files.isDirectory(final.toPath(), LinkOption.NOFOLLOW_LINKS))
            assertEquals("preserve", File(final, "payload.txt").readText())
            assertTrue(Files.isSymbolicLink(File(final, "link").toPath()))
            assertEquals("keep", external.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun ownedRegularStageIsRemovedAfterPostMoveHardLinkRejection() {
        val root = Files.createTempDirectory("anydoc-stage-owned-rejection").toFile()
        val stage = File(root, ".anydoc_stage_owned.part")
        val final = File(root, "report.pdf")
        val alias = File(root, "alias.pdf")
        try {
            stage.writeText("owned")
            reject {
                publishCheckedStagedOutput(stage, final) {
                    Files.createLink(alias.toPath(), stage.toPath())
                }
            }
            assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
            assertEquals("owned", alias.readText())
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
