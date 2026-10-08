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

    @Test fun numbersConsecutivePagesWithoutGaps() {
        val counter = CheckedBatesCounter(7)
        assertEquals(7, counter.takeNext())
        assertEquals(8, counter.takeNext())
        assertEquals(9, counter.takeNext())
    }

    @Test fun continuesSequenceAcrossFiles() {
        val counter = CheckedBatesCounter(100)
        val firstFile = List(2) { counter.takeNext() }
        val secondFile = List(3) { counter.takeNext() }
        assertEquals(listOf(100, 101), firstFile)
        assertEquals(listOf(102, 103, 104), secondFile)
    }

    @Test fun clampsNonPositiveStartToOne() {
        assertEquals(1, CheckedBatesCounter(0).takeNext())
        assertEquals(1, CheckedBatesCounter(-20).takeNext())
    }

    @Test fun rejectsRolloverAcrossFileBoundaryWithoutWrapping() {
        val counter = CheckedBatesCounter(Int.MAX_VALUE - 1)
        assertEquals(Int.MAX_VALUE - 1, counter.takeNext())
        assertEquals(Int.MAX_VALUE, counter.takeNext())
        assertThrows(IllegalArgumentException::class.java) { counter.takeNext() }
        assertThrows(IllegalArgumentException::class.java) { counter.takeNext() }
    }
}
