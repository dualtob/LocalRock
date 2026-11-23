package com.kodraliu.localrock.shared.mqtt

/**
 * Stands in until the mqtt.js transport lands: every connection attempt fails at once, so the
 * App's MQTT supervision reports the Robots as unreachable instead of the page failing to start.
 */
internal object UnavailableMqttTransport : MqttNativeTransport {
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
        onFailed("MQTT is not available in the Web App yet")
        return ClosedConnection
    }

    private object ClosedConnection : MqttNativeConnection {
        override val isConnected: Boolean = false
        override fun subscribe(topic: String, qos: Int) = Unit
        override fun unsubscribe(topic: String) = Unit
        override fun publish(topic: String, payload: ByteArray, qos: Int, retain: Boolean) = Unit
        override fun close() = Unit
    }
}
