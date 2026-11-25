package com.kodraliu.localrock.shared.mqtt

import com.kodraliu.localrock.shared.vacuum.LinkLostException
import com.kodraliu.localrock.shared.vacuum.VacuumSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** What happens around a link that ends: who hears about it, and what stops waiting. */
class MqttClientLinkTest {

    private val creds = MqttCreds(host = "localhost", port = 8883, tls = true, username = "u", password = "p")

    /** Connects every time; [drop] ends the current link the way a closed socket does. */
    private class DroppableTransport : MqttNativeTransport {
        var opens = 0
        val published = CompletableDeferred<Unit>()
        private var onDisconnected: (() -> Unit)? = null

        fun drop() = onDisconnected?.invoke()

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
            opens++
            this.onDisconnected = onDisconnected
            onConnected()
            return object : MqttNativeConnection {
                override val isConnected: Boolean = true
                override fun subscribe(topic: String, qos: Int) {}
                override fun unsubscribe(topic: String) {}
                override fun publish(topic: String, payload: ByteArray, qos: Int, retain: Boolean) {
                    published.complete(Unit)
                }
                override fun close() {}
            }
        }
    }

    @Test
    fun a_link_that_ends_is_counted_once_and_the_client_reconnects() = runTest {
        val transport = DroppableTransport()
        val client = MqttClient(transport, initialBackoffMs = 10)
        try {
            client.start(creds, clientId = "test")
            client.connected.first { it }
            assertEquals(0, client.linkDrops.value)

            transport.drop()
            // Real time: the supervisor runs on its own dispatcher, outside runTest's virtual clock.
            withContext(Dispatchers.Default) {
                withTimeout(5_000) {
                    client.linkDrops.first { it == 1 }
                    while (transport.opens < 2) delay(10)
                    client.connected.first { it }
                }
            }
            assertEquals(1, client.linkDrops.value)
            assertEquals(2, transport.opens)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun a_request_waiting_when_the_link_ends_fails_as_link_lost_at_once() = runTest {
        val transport = DroppableTransport()
        val client = MqttClient(transport, initialBackoffMs = 10)
        val session = VacuumSession(
            localKey = "0123456789abcdef",
            mqtt = client,
            topicsProvider = { MqttTopics(publishTopic = "rr/m/i/x/y/duid", subscribeTopic = "rr/m/o/x/y/duid") },
            nowEpochSeconds = { 1_800_000_000 },
        )
        try {
            client.start(creds, clientId = "test")
            client.connected.first { it }
            session.start()
            // A supervisor scope, so the request's expected failure reaches the assertion instead
            // of cancelling the test.
            withContext(Dispatchers.Default) {
                supervisorScope {
                    // The robot never answers; before, this sat out its whole minute.
                    val request = async { session.sendCommand("get_status", timeoutMs = 60_000) }
                    withTimeout(5_000) { transport.published.await() }
                    transport.drop()
                    val failure = withTimeout(5_000) { assertFailsWith<LinkLostException> { request.await() } }
                    assertEquals("get_status", failure.method)
                }
            }
        } finally {
            session.close()
            client.disconnect()
        }
    }
}
