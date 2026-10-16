package com.docforge.core.pdf

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfStagedLinkCountFallbackTest {
    private val stage = File("injected-stage-path")

    @Test fun knownNioCountDoesNotInvokeAndroidFallback() {
        var fallbackCalls = 0
        val count = stagedHardLinkCount(
            stage,
            readNioCount = { 1L },
            readAndroidCount = { fallbackCalls++; 2L }
        )
        assertEquals(1L, count)
        assertEquals(0, fallbackCalls)
    }

    @Test fun unsupportedNioCountUsesAndroidAliasCount() {
        val count = stagedHardLinkCount(
            stage,
            readNioCount = { null },
            readAndroidCount = { 2L }
        )
        assertEquals(2L, count)
    }

    @Test fun unsupportedNioCountPreservesSingleLinkFallback() {
        val count = stagedHardLinkCount(
            stage,
            readNioCount = { null },
            readAndroidCount = { 1L }
        )
        assertEquals(1L, count)
    }

    @Test fun missingCountsAreNotTreatedAsProofOfUniqueness() {
        val count = stagedHardLinkCount(
            stage,
            readNioCount = { null },
            readAndroidCount = { null }
        )
        assertNull(count)
    }
}
