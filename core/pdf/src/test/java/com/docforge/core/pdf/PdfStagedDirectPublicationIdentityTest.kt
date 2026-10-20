package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Direct publication must establish identity even when callers omit the snapshot. */
class PdfStagedDirectPublicationIdentityTest {
    @Test fun unknownIdentityRejectsBeforeMoveAndPreservesStage() {
        val root = Files.createTempDirectory("direct-unknown-identity").toFile()
        try {
            val stage = File(root, ".anydoc_stage_unknown.part").apply { writeText("owned") }
            val output = File(root, "report.pdf")
            var beforeMoveCalled = false
            try {
                publishCheckedStagedOutput(
                    stage, output,
                    readNioFileKey = { null },
                    readAndroidIdentity = { null },
                    beforeMove = { beforeMoveCalled = true }
                )
                fail("Expected direct publication to reject unknown identity")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("identity"))
            }
            assertFalse(beforeMoveCalled)
            assertEquals("owned", stage.readText())
            assertFalse(Files.exists(output.toPath(), LinkOption.NOFOLLOW_LINKS))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun omittedIdentityUsesAndroidFallbackWhenNioUnavailable() {
        val root = Files.createTempDirectory("direct-android-fallback").toFile()
        try {
            val stage = File(root, ".anydoc_stage_android.part").apply { writeText("owned") }
            val output = File(root, "report.pdf")
            val stableIdentity = StagedAndroidIdentity(5L, 17L)
            publishCheckedStagedOutput(
                stage, output,
                readNioFileKey = { null },
                readAndroidIdentity = { stableIdentity }
            )
            assertFalse(stage.exists())
            assertEquals("owned", output.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun omittedIdentityRejectsAndroidFallbackSwapWithoutDeletingForeignOutput() {
        val root = Files.createTempDirectory("direct-android-swap").toFile()
        try {
            val stage = File(root, ".anydoc_stage_android.part").apply { writeText("owned") }
            val foreign = File(root, "foreign.txt").apply { writeText("foreign") }
            val output = File(root, "report.pdf")
            val identityReader: (File) -> StagedAndroidIdentity? = { file ->
                if (file.readText() == "owned") StagedAndroidIdentity(5L, 17L)
                else StagedAndroidIdentity(5L, 18L)
            }
            try {
                publishCheckedStagedOutput(
                    stage, output,
                    readNioFileKey = { null },
                    readAndroidIdentity = identityReader,
                    beforeMove = {
                        assertTrue(stage.delete())
                        Files.move(foreign.toPath(), stage.toPath())
                    }
                )
                fail("Expected substituted regular file to be rejected")
            } catch (_: IllegalArgumentException) {
                assertEquals("foreign", output.readText())
                assertFalse(Files.exists(stage.toPath(), LinkOption.NOFOLLOW_LINKS))
                assertFalse(foreign.exists())
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
