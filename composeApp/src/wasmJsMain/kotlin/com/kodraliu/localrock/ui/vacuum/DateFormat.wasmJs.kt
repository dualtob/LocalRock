package com.kodraliu.localrock.ui.vacuum

import com.kodraliu.localrock.shared.http.dateNowMs

private fun startOfLocalDayMs(epochMs: Double): Double =
    js("new Date(new Date(epochMs).getFullYear(), new Date(epochMs).getMonth(), new Date(epochMs).getDate()).getTime()")

private fun localTime(epochMs: Double): String =
    js("new Date(epochMs).toLocaleTimeString(undefined, { timeStyle: 'short' })")

private fun localDate(epochMs: Double): String =
    js("new Date(epochMs).toLocaleDateString(undefined, { dateStyle: 'medium' })")

/** `Date.getTimezoneOffset` is minutes *behind* UTC, so its sign is flipped. */
private fun timezoneOffsetMinutes(epochMs: Double): Int = js("-new Date(epochMs).getTimezoneOffset()")

actual fun formatCleanRecordTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "Unknown"
    val ms = epochSeconds * 1000.0
    val day = startOfLocalDayMs(ms)
    val today = startOfLocalDayMs(dateNowMs())
    // One day back from local midnight, taken as a local midnight again so DST days still match.
    val yesterday = startOfLocalDayMs(today - 12 * 3600 * 1000.0)
    val time = localTime(ms)
    return when (day) {
        today -> "Today, $time"
        yesterday -> "Yesterday, $time"
        else -> "${localDate(ms)}, $time"
    }
}

actual fun localUtcOffsetSeconds(epochSeconds: Long): Int =
    timezoneOffsetMinutes(epochSeconds * 1000.0) * 60
