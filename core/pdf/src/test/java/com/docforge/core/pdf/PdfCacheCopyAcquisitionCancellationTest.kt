package com.docforge.core.pdf

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfCacheCopyAcquisitionCancellationTest {
    @Test fun cancelledRegistrationSkipsProviderAndDeletesOwnedTemp() = runBlocking {
        val root = Files.createTempDirectory("anydoc-provider-cancel").toFile()
        val temp = File(root, "owned.tmp").apply { writeText("stage") }
        try {
            ActiveTempFileRegistry.register(temp)
            var opened = false
            var caught = false
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                coroutineContext.cancel()
                try {
                    withCancellationCheckedRegisteredCacheCopy(temp, false) {
                        opened = true
                    }
                    fail("Expected cancellation")
                } catch (_: CancellationException) { caught = true }
            }
            job.join()
            assertTrue(caught)
            assertFalse(opened)
            assertFalse(temp.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun activeAcquisitionReturnsOriginalValue() = runBlocking {
        val root = Files.createTempDirectory("anydoc-provider-active").toFile()
        val temp = File(root, "owned.tmp").apply { writeText("stage") }
        try {
            ActiveTempFileRegistry.register(temp)
            val expected = Any()
            var count = 0
            val result = withCancellationCheckedRegisteredCacheCopy(temp, false) { count++; expected }
            assertSame(expected, result)
            assertEquals(1, count)
            assertTrue(temp.exists())
        } finally {
            ActiveTempFileRegistry.deleteIfInactive(temp)
            root.deleteRecursively()
        }
    }

    @Test fun providerFailurePreservesOriginalErrorAndCleansOwnedTemp() = runBlocking {
        val root = Files.createTempDirectory("anydoc-provider-error").toFile()
        val temp = File(root, "owned.tmp").apply { writeText("stage") }
        try {
            ActiveTempFileRegistry.register(temp)
            val failure = IllegalStateException("provider failed")
            try {
                withCancellationCheckedRegisteredCacheCopy(temp, false) { throw failure }
                fail("Expected provider error")
            } catch (actual: IllegalStateException) { assertSame(failure, actual) }
            assertFalse(temp.exists())
        } finally { root.deleteRecursively() }
    }
}
