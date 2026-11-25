package com.kodraliu.localrock.shared.mqtt


interface MqttNativeConnection {
    val isConnected: Boolean
    fun subscribe(topic: String, qos: Int)
    fun unsubscribe(topic: String)
    fun publish(topic: String, payload: ByteArray, qos: Int, retain: Boolean)
    fun close()

    /**
     * Sends the broker a ping and calls [onAnswer] when it answers, or returns null when this
     * transport can't ping. It proves the link is alive without involving any robot.
     * The caller cancels the returned [MqttPing] once it stops waiting, answered or not.
     */
    fun ping(onAnswer: () -> Unit): MqttPing? = null
}

/** A ping in flight. [cancel] stops listening for its answer; calling it more than once is harmless. */
fun interface MqttPing {
    fun cancel()
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
