package com.kodraliu.localrock.shared.mqtt

import com.kodraliu.localrock.shared.vacuum.LinkLostException
import com.kodraliu.localrock.shared.vacuum.VacuumSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What happens around a link that ends: who hears about it, and what stops waiting. */
class MqttClientLinkTest {

    private val creds = MqttCreds(host = "localhost", port = 8883, tls = true, username = "u", password = "p")

    /** Connects every time; [drop] ends the current link the way a closed socket does. */
    private class DroppableTransport(private val answersPings: Boolean? = null) : MqttNativeTransport {
        var opens = 0
        var published = CompletableDeferred<Unit>()
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
                override fun ping(onAnswer: () -> Unit): Boolean {
                    val answers = answersPings ?: return false
                    if (answers) onAnswer()
                    return true
                }
            }
        }
    }

    /** Never connects, so the supervisor keeps failing and asks for fresh credentials. */
    private class RefusingTransport : MqttNativeTransport {
        var opens = 0
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
            throw RuntimeException("Connection refused")
        }
    }

    @Test
    fun a_link_that_ends_is_counted_once() = runTest {
        val transport = DroppableTransport()
        val client = MqttClient(transport, initialBackoffMs = 10)
        try {
            client.start(creds, clientId = "test")
            client.connected.first { it }
            assertEquals(0, client.linkDrops.value)

            transport.drop()
            withContext(Dispatchers.Default) { withTimeout(5_000) { client.linkDrops.first { it == 1 } } }
            client.connected.first { it }
            assertEquals(1, client.linkDrops.value)
            assertEquals(2, transport.opens)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun a_request_waiting_when_the_link_ends_fails_at_once() = runTest {
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
            // A supervisor scope, so the request's failure reaches the assertion instead of
            // cancelling the test.
            withContext(Dispatchers.Default) { supervisorScope {
                // The robot never answers; before, this sat out its whole minute.
                val request = async { session.sendCommand("get_status", timeoutMs = 60_000) }
                withTimeout(5_000) { transport.published.await() }
                transport.drop()
                val failure = withTimeout(5_000) { assertFailsWith<LinkLostException> { request.await() } }
                assertEquals("get_status", failure.method)
            } }
        } finally {
            session.close()
            client.disconnect()
        }
    }

    @Test
    fun a_login_that_never_finishes_does_not_stop_the_reconnecting() = runTest {
        val transport = RefusingTransport()
        val client = MqttClient(transport, initialBackoffMs = 10, credentialRefreshTimeoutMs = 100)
        var refreshes = 0
        client.credentialRefresher = { _, _ -> refreshes++; awaitCancellation() }
        try {
            client.start(creds, clientId = "test")
            withContext(Dispatchers.Default) {
                // Three failures, a login that hangs, then the attempts must go on regardless.
                withTimeout(5_000) { while (transport.opens < 5) kotlinx.coroutines.delay(10) }
            }
            assertTrue(refreshes >= 1)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun a_ping_tells_a_live_link_from_a_dead_one() = runTest {
        for ((answers, expected) in listOf(true to true, false to false, null to null)) {
            val client = MqttClient(DroppableTransport(answersPings = answers))
            try {
                client.start(creds, clientId = "test")
                client.connected.first { it }
                val result = withContext(Dispatchers.Default) { client.checkLink(timeoutMs = 100) }
                if (expected == null) assertNull(result) else assertEquals(expected, result)
            } finally {
                client.disconnect()
            }
        }
    }

    @Test
    fun silence_is_measured_on_the_wall_clock() = runTest {
        var now = 1_000_000L
        val transport = object : MqttNativeTransport {
            override fun open(
                host: String, port: Int, tls: Boolean, username: String, password: String, clientId: String,
                keepAliveSeconds: Int, onConnected: () -> Unit, onFailed: (reason: String) -> Unit,
                onMessage: (topic: String, payload: ByteArray) -> Unit, onDisconnected: () -> Unit,
            ): MqttNativeConnection {
                onConnected()
                onMessage("t", ByteArray(0))
                return object : MqttNativeConnection {
                    override val isConnected = true
                    override fun subscribe(topic: String, qos: Int) {}
                    override fun unsubscribe(topic: String) {}
                    override fun publish(topic: String, payload: ByteArray, qos: Int, retain: Boolean) {}
                    override fun close() {}
                }
            }
        }
        val client = MqttClient(transport, nowMs = { now })
        try {
            client.start(creds, clientId = "test")
            client.connected.first { it }
            // A phone that slept for ten minutes: a monotonic clock would not have moved.
            now += 600_000L
            assertEquals(600_000L, client.millisSinceLastMessage())
            assertFalse(client.millisSinceLastMessage() < 90_000L)
        } finally {
            client.disconnect()
        }
    }
}
