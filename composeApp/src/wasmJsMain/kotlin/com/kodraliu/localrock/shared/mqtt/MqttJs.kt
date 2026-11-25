@file:JsModule("mqtt")

package com.kodraliu.localrock.shared.mqtt

/** mqtt.js, bundled with the Web App (npm `mqtt`); its browser build only has a default export. */
@JsName("default")
internal external object MqttJs : JsAny {
    fun connect(brokerUrl: String, options: JsAny): MqttJsClient
}

internal external interface MqttJsClient : JsAny {
    val connected: Boolean
    fun subscribe(topic: String, options: JsAny): MqttJsClient
    fun unsubscribe(topic: String): MqttJsClient
    fun publish(topic: String, message: JsAny, options: JsAny): MqttJsClient
    fun end(force: Boolean): MqttJsClient
    fun sendPing()
}
