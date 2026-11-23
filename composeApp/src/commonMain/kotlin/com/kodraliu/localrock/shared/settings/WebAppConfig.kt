package com.kodraliu.localrock.shared.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The Server the Web App's deployment names, read once at startup (ADR 0002). */
sealed interface WebAppServer {
    data class Set(val url: String) : WebAppServer
    data object NotSet : WebAppServer
}

/**
 * Reads the text of the Web App's `config.json`, e.g. `{"serverUrl": "https://robots.lan:555"}`,
 * or null when there is no such file. A missing file, invalid JSON or a missing or blank
 * `serverUrl` all mean the deployment set no Server.
 */
fun webAppServer(configJson: String?): WebAppServer {
    val config = configJson
        ?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() as? JsonObject }
        ?: return WebAppServer.NotSet
    val url = (config["serverUrl"] as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.content
        ?.trim()
    return if (url.isNullOrEmpty()) WebAppServer.NotSet else WebAppServer.Set(url)
}
