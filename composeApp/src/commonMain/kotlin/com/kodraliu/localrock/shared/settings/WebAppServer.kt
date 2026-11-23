package com.kodraliu.localrock.shared.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The Server the Web App's deployment names, read once at startup (ADR 0002), or null when it
 * names none. Reads the text of the Web App's `config.json`, e.g.
 * `{"serverUrl": "https://robots.lan:555"}`, or null when there is no such file. A missing file,
 * invalid JSON or a missing or blank `serverUrl` all mean the deployment set no Server.
 */
fun webAppServer(configJson: String?): String? {
    val config = configJson
        ?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() as? JsonObject }
        ?: return null
    return (config["serverUrl"] as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.content
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}
