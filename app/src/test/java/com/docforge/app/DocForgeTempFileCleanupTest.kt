package com.docforge.app

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
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

    @Test fun staleFileBecomesReclaimableAfterItsOwnerUnregisters() {
        val stale = temp("docforge_released.tmp", ageMillis * 2L)
        ActiveTempFileRegistry.register(stale)
        try {
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
            assertTrue(stale.exists())
        } finally {
            ActiveTempFileRegistry.unregister(stale)
        }
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
        assertFalse(stale.exists())
    }

    @Test fun unrelatedFilesAndFutureTimestampArePreserved() {
        val unrelated = temp("keep_user_data.txt", ageMillis * 2L)
        val future = temp("docforge_future.tmp", -60_000L)
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
        assertTrue(unrelated.exists())
        assertTrue(future.exists())
    }

    @Test fun activeStaleFileAndInactiveSiblingAreHandledIndependently() {
        val active = temp("docforge_guarded.tmp", ageMillis * 2)
        val inactive = temp("docforge_unregistered.tmp", ageMillis * 2)
        ActiveTempFileRegistry.register(active)
        try {
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
            assertTrue(active.exists())
            assertFalse(inactive.exists())
        } finally {
            ActiveTempFileRegistry.unregister(active)
        }
    }

    @Test fun registrationBeforeDeletionProtectsStaleFile() {
        val stale = temp("docforge_registered_first.tmp", ageMillis * 2)
        val pool = Executors.newSingleThreadExecutor()
        try {
            // Registration must finish before cleanup begins.
            pool.submit { ActiveTempFileRegistry.register(stale) }.get(5, TimeUnit.SECONDS)
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, ageMillis)
            assertTrue(stale.exists())
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(stale))
            assertTrue(stale.exists())
        } finally {
            ActiveTempFileRegistry.unregister(stale)
            pool.shutdownNow()
        }
    }

    @Test fun registrationCannotInterleaveWhileDeletionHoldsRegistryLock() {
        val actual = temp("docforge_delete_first.tmp", ageMillis * 2)
        val enteredDelete = CountDownLatch(1)
        val allowDelete = CountDownLatch(1)
        val registerStarted = CountDownLatch(1)
        val registered = CountDownLatch(1)
        val intercepted = object : File(actual.absolutePath) {
            override fun delete(): Boolean {
                enteredDelete.countDown()
                check(allowDelete.await(5, TimeUnit.SECONDS))
                return super.delete()
            }
        }
        val pool = Executors.newFixedThreadPool(2)
        try {
            val deletion = pool.submit<Boolean> {
                ActiveTempFileRegistry.deleteIfInactive(intercepted)
            }
            assertTrue(enteredDelete.await(5, TimeUnit.SECONDS))
            val registration = pool.submit {
                registerStarted.countDown()
                ActiveTempFileRegistry.register(actual)
                registered.countDown()
            }
            assertTrue(registerStarted.await(5, TimeUnit.SECONDS))
            // The old implementation would allow registration to finish inside
            // the gap between isActive() and file.delete().
            assertFalse(registered.await(100, TimeUnit.MILLISECONDS))
            allowDelete.countDown()
            assertTrue(deletion.get(5, TimeUnit.SECONDS))
            registration.get(5, TimeUnit.SECONDS)
            assertEquals(0L, registered.count)
            assertFalse(actual.exists())
        } finally {
            allowDelete.countDown()
            ActiveTempFileRegistry.unregister(actual)
            pool.shutdownNow()
        }
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
