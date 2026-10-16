package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class PdfStagedHardLinkTest {
    @Test fun ordinarySingleLinkStagePublishes() {
        val root = Files.createTempDirectory("stage-link-normal").toFile()
        try {
            val stage = File(root, ".anydoc_stage_regular.part").apply { writeText("safe") }
            val output = File(root, "report.pdf")
            publishCheckedStagedOutput(stage, output)
            assertEquals("safe", output.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun knownHardLinkedStageIsRejectedWithoutMutatingOtherLink() {
        val root = Files.createTempDirectory("stage-link-shared").toFile()
        try {
            val external = File(root, "unrelated.txt").apply { writeText("preserve") }
            val stage = File(root, ".anydoc_stage_hard.part")
            Files.createLink(stage.toPath(), external.toPath())
            assumeTrue((stagedHardLinkCount(stage) ?: 0L) > 1L)
            val final = File(root, "report.pdf")
            try {
                publishCheckedStagedOutput(stage, final)
                fail("Expected a known hard-link alias to be rejected")
            } catch (_: IllegalArgumentException) {
                assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
                assertEquals("preserve", external.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun swapToHardLinkBeforeRenameCannotLeavePublishedAlias() {
        val root = Files.createTempDirectory("stage-link-swap").toFile()
        try {
            val external = File(root, "unrelated.txt").apply { writeText("preserve") }
            val stage = File(root, ".anydoc_stage_hard.part").apply { writeText("original") }
            val final = File(root, "report.pdf")
            assumeTrue(stagedHardLinkCount(stage) != null)
            try {
                publishCheckedStagedOutput(stage, final) {
                    assertTrue(stage.delete())
                    Files.createLink(stage.toPath(), external.toPath())
                }
                fail("Expected substituted hard-link alias rejection")
            } catch (_: IllegalArgumentException) {
                assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
                assertEquals("preserve", external.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun countUnavailableIsNotMisrepresentedAsProofOfSafety() {
        val root = Files.createTempDirectory("stage-link-count").toFile()
        try {
            val stage = File(root, ".anydoc_stage_regular.part").apply { writeText("normal") }
            val count = stagedHardLinkCount(stage)
            assertTrue(count == null || count == 1L)
        } finally {
            root.deleteRecursively()
        }
    }
}
