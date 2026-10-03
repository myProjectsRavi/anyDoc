package com.docforge.feature.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioPcmCachePreflightTest {

    @Test
    fun requiredPcmCacheBytes_estimatesRepresentativeStereoMinute() {
        val required = requiredPcmCacheBytes(
            durationMs = 60_000L,
            sampleRateHz = 44_100,
            channelCount = 2
        )

        val expectedPcmBytes = 60L * 44_100L * 2L * 2L
        assertEquals(expectedPcmBytes + AUDIO_PCM_CACHE_RESERVE_BYTES, required)
    }

    @Test
    fun requiredPcmCacheBytes_usesConservativeDefaultsWhenRateOrChannelsMissing() {
        val required = requiredPcmCacheBytes(
            durationMs = 1_000L,
            sampleRateHz = null,
            channelCount = null
        )

        val expectedPcmBytes = 48_000L * 2L * 2L
        assertEquals(expectedPcmBytes + AUDIO_PCM_CACHE_RESERVE_BYTES, required)
    }

    @Test
    fun requiredPcmCacheBytes_skipsFalsePrecisionWhenDurationUnknownOrInvalid() {
        assertNull(requiredPcmCacheBytes(null, 44_100, 2))
        assertNull(requiredPcmCacheBytes(0L, 44_100, 2))
        assertNull(requiredPcmCacheBytes(-1L, 44_100, 2))
    }

    @Test
    fun requiredPcmCacheBytes_saturatesPathologicalMetadataInsteadOfOverflowing() {
        assertEquals(
            Long.MAX_VALUE,
            requiredPcmCacheBytes(
                durationMs = Long.MAX_VALUE,
                sampleRateHz = Int.MAX_VALUE,
                channelCount = Int.MAX_VALUE
            )
        )
    }
}
