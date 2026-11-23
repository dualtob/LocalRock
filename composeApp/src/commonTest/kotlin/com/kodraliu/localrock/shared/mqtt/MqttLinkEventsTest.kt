package com.kodraliu.localrock.shared.mqtt

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A browser MQTT library reports `connect`, `error` and `close` in its own order; the transport
 * callbacks must see exactly one outcome per link: connected then disconnected, or failed.
 */
class MqttLinkEventsTest {

    private val seen = mutableListOf<String>()
    private val events = MqttLinkEvents(
        onConnected = { seen += "connected" },
        onFailed = { seen += "failed: $it" },
        onDisconnected = { seen += "disconnected" },
    )

    @Test
    fun linkThatConnectsAndLaterClosesReportsConnectedThenDisconnectedOnce() {
        events.connected()
        events.closed()
        events.closed()
        assertEquals(listOf("connected", "disconnected"), seen)
    }

    @Test
    fun refusedLoginIsOneFailureCarryingTheReason() {
        events.error("Connection refused: Bad username or password")
        events.closed()
        assertEquals(listOf("failed: Connection refused: Bad username or password"), seen)
    }

    @Test
    fun socketThatClosesBeforeConnectingIsAFailure() {
        events.closed()
        assertEquals(listOf("failed: connection closed before MQTT connected"), seen)
    }

    @Test
    fun errorOnAnEstablishedLinkWaitsForTheCloseThatFollows() {
        events.connected()
        events.error("keepalive timeout")
        events.closed()
        assertEquals(listOf("connected", "disconnected"), seen)
    }
}
