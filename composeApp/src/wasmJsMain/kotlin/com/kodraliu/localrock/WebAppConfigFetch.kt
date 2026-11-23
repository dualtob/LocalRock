package com.kodraliu.localrock

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Response

/**
 * The text at [url] (the Web App's `config.json` beside the page), or null when it can't be
 * fetched. The image serves it with `Cache-Control: no-cache`. No `RequestInit` here: on
 * Kotlin/Wasm it sends every field it isn't given as `null`, which browsers reject
 * (`credentials: null`), so the fetch always failed and the Web App said it had no Server.
 */
internal suspend fun fetchConfigJson(url: String = "config.json"): String? = try {
    val response = window.fetch(url).await<Response>()
    if (response.ok) response.text().await<JsString>().toString() else null
} catch (e: Throwable) {
    println("[LocalRock] couldn't read $url: $e")
    null
}
