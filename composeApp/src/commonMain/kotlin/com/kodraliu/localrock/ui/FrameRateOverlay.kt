package com.kodraliu.localrock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Frames per second the App actually draws, and how many of them came late in the last second.
 * For telling 60 from 120 Hz on a ProMotion iPhone, and for finding where the App stutters.
 */
@Composable
fun FrameRateOverlay(modifier: Modifier = Modifier) {
    var reading by remember { mutableStateOf(FrameReading(0, 0)) }
    LaunchedEffect(Unit) {
        val counter = FrameCounter()
        while (true) {
            withFrameNanos { now -> counter.frame(now)?.let { reading = it } }
        }
    }
    Text(
        text = "${reading.fps} fps" + if (reading.late > 0) " · ${reading.late} late" else "",
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

internal data class FrameReading(val fps: Int, val late: Int)

/**
 * Counts frames per second from their timestamps. A frame is late when it came more than 1.5 of
 * the second's shortest gap after the one before: one or more frames were skipped there.
 */
internal class FrameCounter {
    private var windowStart = -1L
    private var last = -1L
    private val gaps = mutableListOf<Long>()

    /** Takes one frame's time; returns a reading when a second has passed, else null. */
    fun frame(nanos: Long): FrameReading? {
        if (windowStart < 0) {
            windowStart = nanos
            last = nanos
            return null
        }
        gaps += nanos - last
        last = nanos
        val elapsed = nanos - windowStart
        if (elapsed < 1_000_000_000L) return null
        val shortest = gaps.min().coerceAtLeast(1)
        val reading = FrameReading(
            fps = ((gaps.size * 1_000_000_000L + elapsed / 2) / elapsed).toInt(),
            late = gaps.count { it * 2 > shortest * 3 },
        )
        gaps.clear()
        windowStart = nanos
        return reading
    }
}
