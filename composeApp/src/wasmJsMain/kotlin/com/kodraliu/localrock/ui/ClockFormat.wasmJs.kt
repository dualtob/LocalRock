package com.kodraliu.localrock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private fun browserHourCycle(): String =
    js("Intl.DateTimeFormat(undefined, { hour: 'numeric' }).resolvedOptions().hourCycle || ''")

@Composable
actual fun rememberUse24HourClock(): Boolean = remember {
    browserHourCycle().let { it == "h23" || it == "h24" }
}
