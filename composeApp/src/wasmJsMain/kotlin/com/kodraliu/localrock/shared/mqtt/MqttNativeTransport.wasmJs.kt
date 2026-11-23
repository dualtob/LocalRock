package com.kodraliu.localrock.shared.mqtt

/** None by default: the Web App's entry point passes [webAppMqttTransport] for its page's origin. */
actual fun defaultMqttTransport(): MqttNativeTransport? = null

/** The Web App's MQTT: mqtt.js over a WebSocket to `/mqtt` on the Server, which is [pageOrigin]. */
fun webAppMqttTransport(pageOrigin: String): MqttNativeTransport =
    MqttJsTransport(mqttWebSocketUrl(pageOrigin), pageId = randomPageId())

/**
 * MQTT over WebSockets with mqtt.js. Browsers can't open the raw TLS socket the App uses, so the
 * Server relays `/mqtt` to its broker (ADR 0001). The host, port and TLS flag that [MqttClient]
 * derives from `rriot` are ignored: everything goes to [url] with the same username and password.
 * mqtt.js's own reconnecting is off; [MqttClient] supervises the link, as it does on the App.
 */
internal class MqttJsTransport(
    internal val url: String,
    /** Keeps two tabs of the same login from sharing a client ID, see [webPageMqttClientId]. */
    private val pageId: String,
) : MqttNativeTransport {

    override fun open(
        host: String,
        port: Int,
        tls: Boolean,
        username: String,
        password: String,
        clientId: String,
        keepAliveSeconds: Int,
        onConnected: () -> Unit,
        onFailed: (reason: String) -> Unit,
        onMessage: (topic: String, payload: ByteArray) -> Unit,
        onDisconnected: () -> Unit,
    ): MqttNativeConnection {
        val events = MqttLinkEvents(onConnected, onFailed, onDisconnected)
        val client = MqttJs.connect(
            url,
            connectOptions(
                username = username,
                password = password,
                clientId = webPageMqttClientId(clientId, pageId),
                keepalive = keepAliveSeconds,
                connectTimeout = CONNECT_TIMEOUT_MS,
            ),
        )
        whenConnected(client) { events.connected() }
        whenErrored(client) { reason -> events.error(reason) }
        whenClosed(client) { events.closed() }
        whenMessage(client) { topic, payload -> onMessage(topic, payload.toByteArray()) }
        return MqttJsConnection(client)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 20_000
    }
}

private class MqttJsConnection(private val client: MqttJsClient) : MqttNativeConnection {
    override val isConnected: Boolean get() = client.connected

    override fun subscribe(topic: String, qos: Int) {
        client.subscribe(topic, qosOptions(qos, retain = false))
    }

    override fun unsubscribe(topic: String) {
        client.unsubscribe(topic)
    }

    override fun publish(topic: String, payload: ByteArray, qos: Int, retain: Boolean) {
        client.publish(topic, payload.toUint8Array(), qosOptions(qos, retain))
    }

    override fun close() {
        client.end(true)
    }
}

private fun connectOptions(
    username: String,
    password: String,
    clientId: String,
    keepalive: Int,
    connectTimeout: Int,
): JsAny = js(
    """({
        username: username,
        password: password,
        clientId: clientId,
        keepalive: keepalive,
        connectTimeout: connectTimeout,
        protocolVersion: 4,
        clean: true,
        reconnectPeriod: 0
    })"""
)

private fun randomPageId(): String = js("Math.random().toString(36).slice(2, 10)")

private fun qosOptions(qos: Int, retain: Boolean): JsAny = js("({ qos: qos, retain: retain })")

private fun whenConnected(client: MqttJsClient, listener: () -> Unit): Unit =
    js("{ client.on('connect', () => listener()); }")

private fun whenErrored(client: MqttJsClient, listener: (String) -> Unit): Unit =
    js("{ client.on('error', (e) => listener(String((e && e.message) || e))); }")

private fun whenClosed(client: MqttJsClient, listener: () -> Unit): Unit =
    js("{ client.on('close', () => listener()); }")

private fun whenMessage(client: MqttJsClient, listener: (String, JsAny) -> Unit): Unit =
    js("{ client.on('message', (topic, payload) => listener(topic, payload)); }")

private fun byteLength(bytes: JsAny): Int = js("bytes.length")

private fun byteAt(bytes: JsAny, index: Int): Int = js("bytes[index]")

private fun newUint8Array(size: Int): JsAny = js("new Uint8Array(size)")

private fun setByte(bytes: JsAny, index: Int, value: Int): Unit = js("{ bytes[index] = value; }")

private fun JsAny.toByteArray(): ByteArray = ByteArray(byteLength(this)) { byteAt(this, it).toByte() }

private fun ByteArray.toUint8Array(): JsAny {
    val out = newUint8Array(size)
    for (i in indices) setByte(out, i, this[i].toInt() and 0xff)
    return out
}
