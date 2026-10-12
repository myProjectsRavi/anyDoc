package com.docforge.app

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DocForgeTempFileCleanupTest {
    @get:Rule val folder = TemporaryFolder()

    private val nowMillis = 1_800_000_000_000L
    private val ageMillis = 30L * 60L * 1000L

    private fun temp(name: String, age: Long): File =
        folder.newFile(name).apply {
            writeText("in use")
            assertTrue(setLastModified(nowMillis - age))
        }

    @Test fun freshUnregisteredTempSurvivesMemoryTrim() {
        val fresh = temp("docforge_fresh.tmp", ageMillis - 1L)
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
        assertTrue(fresh.exists())
    }

    @Test fun staleInactiveTempIsReclaimed() {
        val stale = temp("docforge_stale.tmp", ageMillis + 1L)
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
        assertFalse(stale.exists())
    }

    @Test fun exactAgeBoundaryIsReclaimed() {
        val atBoundary = temp("docforge_boundary.tmp", ageMillis)
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
        assertFalse(atBoundary.exists())
    }

    @Test fun activeRegisteredTempSurvivesEvenWhenStale() {
        val active = temp("docforge_active.tmp", ageMillis + 100_000L)
        ActiveTempFileRegistry.register(active)
        try {
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
            assertTrue(active.exists())
        } finally {
            ActiveTempFileRegistry.unregister(active)
        }
    }

    @Test fun unrelatedFilesAndFutureTimestampArePreserved() {
        val unrelated = temp("keep_user_data.txt", ageMillis * 2L)
        val future = temp("docforge_future.tmp", -60_000L)
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
        assertTrue(unrelated.exists())
        assertTrue(future.exists())
    }

    @Test fun invalidAgeDoesNotDeleteFiles() {
        val stale = temp("docforge_stale.tmp", ageMillis * 2L)
        try {
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, -1L)
            throw AssertionError("Expected invalid cleanup age")
        } catch (_: IllegalArgumentException) {
            assertTrue(stale.exists())
        }
    }
}
