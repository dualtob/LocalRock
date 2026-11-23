package com.kodraliu.localrock.shared.http

private fun dateNowMs(): Double = js("Date.now()")

internal actual fun currentEpochSeconds(): Long = (dateNowMs() / 1000.0).toLong()
