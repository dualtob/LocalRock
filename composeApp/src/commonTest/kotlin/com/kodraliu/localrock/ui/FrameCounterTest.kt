package com.kodraliu.localrock.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// One frame at 60 Hz and at 120 Hz, rounded up so that 60 and 120 of them reach a full second.
private const val GAP_60_HZ = 16_666_667L
private const val GAP_120_HZ = 8_333_334L

class FrameCounterTest {
    @Test
    fun there_is_no_reading_before_a_full_second() {
        val counter = FrameCounter()
        // Four frames a second: three gaps of 250 ms come to 750 ms, short of a second.
        assertNull(counter.frame(0L))
        assertNull(counter.frame(250_000_000L))
        assertNull(counter.frame(500_000_000L))
        assertNull(counter.frame(750_000_000L))
        // The fourth gap ends exactly one second after the first frame.
        assertEquals(FrameReading(fps = 4, late = 0), counter.frame(1_000_000_000L))
    }

    @Test
    fun a_steady_60_hz_reads_60_with_none_late() {
        // 60 gaps of 16.67 ms end at 1.00000002 s, the first frame to reach a full second.
        assertEquals(listOf(FrameReading(fps = 60, late = 0)), readings(List(60) { GAP_60_HZ }))
    }

    @Test
    fun a_steady_120_hz_reads_120_with_none_late() {
        // 120 gaps of 8.33 ms end at 1.00000008 s.
        assertEquals(listOf(FrameReading(fps = 120, late = 0)), readings(List(120) { GAP_120_HZ }))
    }

    @Test
    fun a_frame_after_a_skipped_one_counts_as_late() {
        // At 60 Hz, three frames each come one frame late: 54 regular gaps and 3 double ones fill
        // the same 60 frame slots, ending at 1.00000002 s, with 57 frames drawn.
        val gaps = List(18) { GAP_60_HZ } + 2 * GAP_60_HZ +
            List(18) { GAP_60_HZ } + 2 * GAP_60_HZ +
            List(18) { GAP_60_HZ } + 2 * GAP_60_HZ
        assertEquals(listOf(FrameReading(fps = 57, late = 3)), readings(gaps))
    }

    /** Feeds a frame at time 0 and then one after each of [gaps]; returns every reading given. */
    private fun readings(gaps: List<Long>): List<FrameReading> {
        val counter = FrameCounter()
        var now = 0L
        val result = listOfNotNull(counter.frame(now)).toMutableList()
        for (gap in gaps) {
            now += gap
            counter.frame(now)?.let { result += it }
        }
        return result
    }
}
