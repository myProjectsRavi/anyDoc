package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfBatchStampBatesSequenceTest {
    @Test fun acceptsMaximumSequence() {
        assertEquals(Int.MAX_VALUE, checkedBatesSequenceNumber(Int.MAX_VALUE.toLong()))
    }

    @Test fun rejectsRollover() {
        assertThrows(IllegalArgumentException::class.java) {
            checkedBatesSequenceNumber(Int.MAX_VALUE.toLong() + 1L)
        }
    }
}
