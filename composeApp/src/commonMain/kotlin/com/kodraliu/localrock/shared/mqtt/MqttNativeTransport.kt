package com.kodraliu.localrock.shared.mqtt


interface MqttNativeConnection {
    val isConnected: Boolean
    fun subscribe(topic: String, qos: Int)
    fun unsubscribe(topic: String)
    fun publish(topic: String, payload: ByteArray, qos: Int, retain: Boolean)
    fun close()

    /**
     * Sends the broker a ping and calls [onAnswer] when it answers, or returns false when this
     * transport can't do that. It proves the link is alive without involving any robot.
     */
    fun ping(onAnswer: () -> Unit): Boolean = false
}


interface MqttNativeTransport {
    fun open(
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
    ): MqttNativeConnection
}


expect fun defaultMqttTransport(): MqttNativeTransport?
