package com.kodraliu.localrock.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FrameCounterTest {

    private fun run(frameGapsNanos: List<Long>): FrameReading? {
        val counter = FrameCounter()
        var now = 0L
        var reading: FrameReading? = counter.frame(now)
        for (gap in frameGapsNanos) {
            now += gap
            counter.frame(now)?.let { reading = it }
        }
        return reading
    }

    @Test
    fun a_steady_120_hz_second_reads_120_with_none_late() {
        assertEquals(FrameReading(fps = 120, late = 0), run(List(120) { 8_333_334L }))
    }

    @Test
    fun a_second_capped_at_60_reads_60() {
        assertEquals(FrameReading(fps = 60, late = 0), run(List(60) { 16_666_667L }))
    }

    @Test
    fun skipped_frames_are_counted_as_late() {
        // 119 frames in just over a second, two of which each came after a skipped one.
        val gaps = List(58) { 8_333_334L } + 16_666_667L + List(59) { 8_333_334L } + 16_666_667L
        assertEquals(FrameReading(fps = 118, late = 2), run(gaps))
    }

    @Test
    fun nothing_is_read_before_a_second_has_passed() {
        assertNull(run(List(10) { 8_333_333L }))
    }
}
