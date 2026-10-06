package com.docforge.feature.converter

import android.media.MediaFormat
import android.test.mock.MockContext
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class VideoAudioExtractorTest {

    private val extractor = VideoAudioExtractor(MockContext())

    @Test
    fun selectBufferSizeUsesFallbackWhenMetadataIsAbsent() {
        assertEquals(256 * 1024, extractor.selectBufferSize(MediaFormat()))
    }

    @Test
    fun selectBufferSizeRaisesSmallMetadataToFallback() {
        val format = MediaFormat().apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
        }

        assertEquals(256 * 1024, extractor.selectBufferSize(format))
    }

    @Test
    fun selectBufferSizePreservesOrdinaryAndMaximumValues() {
        val ordinary = MediaFormat().apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024)
        }
        val maximum = MediaFormat().apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8 * 1024 * 1024)
        }

        assertEquals(1024 * 1024, extractor.selectBufferSize(ordinary))
        assertEquals(8 * 1024 * 1024, extractor.selectBufferSize(maximum))
    }

    @Test
    fun selectBufferSizeRejectsMetadataAboveMaximum() {
        val format = MediaFormat().apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, (8 * 1024 * 1024) + 1)
        }

        try {
            extractor.selectBufferSize(format)
            fail("Expected oversized sample-buffer metadata to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
