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

private const val NANOS_PER_SECOND = 1_000_000_000L

/**
 * Frames per second the Web App actually draws, and how many of them came late in the last second.
 * For telling 60 from 120 Hz on a ProMotion iPhone, and for finding where the Web App stutters.
 */
@Composable
fun FrameRateOverlay(modifier: Modifier = Modifier) {
    var reading by remember { mutableStateOf<FrameReading?>(null) }
    LaunchedEffect(Unit) {
        val counter = FrameCounter()
        while (true) {
            withFrameNanos { now -> counter.frame(now)?.let { reading = it } }
        }
    }
    val shown = reading ?: return
    Text(
        text = "${shown.fps} fps" + if (shown.late > 0) " · ${shown.late} late" else "",
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
 * Counts frames per second from their timestamps. A frame is late when its gap exceeds 1.5 times
 * the second's shortest gap: one or more frames were skipped there.
 */
internal class FrameCounter {
    private var windowStart = -1L
    private var last = -1L
    private val gaps = mutableListOf<Long>()

    /** Takes one frame's time; returns a reading once a full second has passed, else null. */
    fun frame(nanos: Long): FrameReading? {
        if (windowStart < 0) {
            windowStart = nanos
            last = nanos
            return null
        }
        gaps += nanos - last
        last = nanos
        val elapsed = nanos - windowStart
        if (elapsed < NANOS_PER_SECOND) return null
        val shortest = gaps.min().coerceAtLeast(1)
        val reading = FrameReading(
            fps = ((gaps.size * NANOS_PER_SECOND + elapsed / 2) / elapsed).toInt(),
            late = gaps.count { it * 2 > shortest * 3 },
        )
        gaps.clear()
        windowStart = nanos
        return reading
    }
}
