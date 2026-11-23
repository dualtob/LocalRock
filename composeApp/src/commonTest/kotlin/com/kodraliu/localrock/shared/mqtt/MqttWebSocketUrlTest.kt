package com.kodraliu.localrock.shared.mqtt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The Web App reaches MQTT at `/mqtt` on the page's own origin, never at the host in `rriot`. */
class MqttWebSocketUrlTest {

    @Test
    fun httpsPageWithPortUsesWssOnTheSamePort() {
        assertEquals(
            "wss://api-roborock.dinglreiter.eu:555/mqtt",
            mqttWebSocketUrl("https://api-roborock.dinglreiter.eu:555"),
        )
    }

    @Test
    fun httpsPageOnTheDefaultPortKeepsNoPort() {
        assertEquals("wss://robots.example/mqtt", mqttWebSocketUrl("https://robots.example"))
    }

    @Test
    fun plainHttpPageUsesPlainWs() {
        assertEquals("ws://localhost:8080/mqtt", mqttWebSocketUrl("http://localhost:8080"))
    }

    @Test
    fun trailingSlashOnTheOriginIsIgnored() {
        assertEquals("wss://localhost:8555/mqtt", mqttWebSocketUrl("https://localhost:8555/"))
    }

    /** The broker drops the older of two links with one client ID, so two tabs would take turns. */
    @Test
    fun twoPagesOfTheSameLoginGetDifferentClientIds() {
        val first = webPageMqttClientId("vaclocal-rruid-install", pageId = "a1b2c3")
        val second = webPageMqttClientId("vaclocal-rruid-install", pageId = "d4e5f6")
        assertNotEquals(first, second)
        assertTrue(first.startsWith("vaclocal-rruid-install"))
    }
}
