package com.kodraliu.localrock.shared.mqtt

/**
 * Where the Web App reaches MQTT: `/mqtt` on the [server] its deployment names, over WebSockets.
 * The host and port in `rriot` are for the App's raw TLS link and are not used here.
 */
fun mqttWebSocketUrl(server: String): String {
    val origin = server.trimEnd('/')
    val wsOrigin = when {
        origin.startsWith("https://") -> "wss://" + origin.removePrefix("https://")
        origin.startsWith("http://") -> "ws://" + origin.removePrefix("http://")
        else -> origin
    }
    return "$wsOrigin/mqtt"
}

/**
 * The App's client ID is per install, which the Web App's browser storage shares across tabs. The
 * broker drops the older of two links with the same ID, so each page adds its own [pageId].
 */
fun webPageMqttClientId(clientId: String, pageId: String): String = "$clientId-$pageId"

/** Turns a browser MQTT library's `connect`, `error` and `close` events into one outcome per link. */
class MqttLinkEvents(
    private val onConnected: () -> Unit,
    private val onFailed: (reason: String) -> Unit,
    private val onDisconnected: () -> Unit,
) {
    private var linked = false
    private var finished = false

    fun connected() {
        if (finished || linked) return
        linked = true
        onConnected()
    }

    fun error(reason: String) {
        if (finished || linked) return
        finished = true
        onFailed(reason)
    }

    fun closed() {
        if (finished) return
        finished = true
        if (linked) onDisconnected() else onFailed("connection closed before MQTT connected")
    }
}
