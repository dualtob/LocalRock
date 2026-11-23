package com.kodraliu.localrock.shared.http

/** Milliseconds since the epoch, from the browser clock. */
internal fun dateNowMs(): Double = js("Date.now()")

internal actual fun currentEpochSeconds(): Long = (dateNowMs() / 1000.0).toLong()
