package com.docforge.feature.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class VideoAudioExtractorTest {

    @Test
    fun selectBufferSizeUsesFallbackWhenMetadataIsAbsent() {
        assertEquals(256 * 1024, selectExtractorBufferSize(null))
    }

    @Test
    fun selectBufferSizeRaisesSmallMetadataToFallback() {
        assertEquals(256 * 1024, selectExtractorBufferSize(64 * 1024))
    }

    @Test
    fun selectBufferSizePreservesOrdinaryAndMaximumValues() {
        assertEquals(1024 * 1024, selectExtractorBufferSize(1024 * 1024))
        assertEquals(8 * 1024 * 1024, selectExtractorBufferSize(8 * 1024 * 1024))
    }

    @Test
    fun selectBufferSizeRejectsMetadataAboveMaximum() {
        try {
            selectExtractorBufferSize((8 * 1024 * 1024) + 1)
            fail("Expected oversized sample-buffer metadata to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
