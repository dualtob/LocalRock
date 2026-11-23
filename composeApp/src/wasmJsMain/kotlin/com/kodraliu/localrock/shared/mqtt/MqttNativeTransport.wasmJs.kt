package com.kodraliu.localrock.shared.mqtt

// The mqtt.js-backed transport comes in a later ticket; until then the Web App has no MQTT.
actual fun defaultMqttTransport(): MqttNativeTransport? = null
