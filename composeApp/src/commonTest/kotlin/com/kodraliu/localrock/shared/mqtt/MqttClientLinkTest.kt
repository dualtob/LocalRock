package com.kodraliu.localrock.shared.mqtt

import com.kodraliu.localrock.shared.vacuum.LinkLostException
import com.kodraliu.localrock.shared.vacuum.VacuumSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What happens around a link that ends: who hears about it, and what stops waiting. */
class MqttClientLinkTest {

    private val creds = MqttCreds(host = "localhost", port = 8883, tls = true, username = "u", password = "p")

    /**
     * Connects every time; [drop] ends the current link the way a closed socket does.
     * [answersPings]: true, the broker answers a ping; false, it stays silent; null, no ping at all.
     */
    private class DroppableTransport(private val answersPings: Boolean? = null) : MqttNativeTransport {
        var opens = 0
        val published = CompletableDeferred<Unit>()
        /** Pings sent and not yet answered or cancelled: each one holds a listener on the real client. */
        var pendingPings = 0
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
                override fun ping(onAnswer: () -> Unit): MqttPing? {
                    val answers = answersPings ?: return null
                    if (answers) {
                        onAnswer()
                        return MqttPing {}
                    }
                    pendingPings++
                    return MqttPing { pendingPings-- }
                }
            }
        }
    }

    /** Never connects, so the supervisor keeps failing and in time asks for fresh credentials. */
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

    @Test
    fun a_request_sent_on_the_new_link_survives_a_late_look_at_the_drop() = runTest {
        val transport = DroppableTransport()
        val client = MqttClient(transport, initialBackoffMs = 10)
        // The link watcher runs only when told to, so it can look at the drop late, as it may in real life.
        val watcher = StandardTestDispatcher()
        val watcherScope = CoroutineScope(SupervisorJob() + watcher)
        val session = VacuumSession(
            localKey = "0123456789abcdef",
            mqtt = client,
            topicsProvider = { MqttTopics(publishTopic = "rr/m/i/x/y/duid", subscribeTopic = "rr/m/o/x/y/duid") },
            nowEpochSeconds = { 1_800_000_000 },
            scope = watcherScope,
        )
        try {
            client.start(creds, clientId = "test")
            client.connected.first { it }
            session.start()
            withContext(Dispatchers.Default) {
                supervisorScope {
                    transport.drop()
                    withTimeout(5_000) {
                        client.linkDrops.first { it == 1 }
                        while (transport.opens < 2) delay(10)
                        client.connected.first { it }
                    }
                    // Registered and sent on the new link, before the watcher has seen the drop.
                    val request = async { session.sendCommand("get_status", timeoutMs = 60_000) }
                    withTimeout(5_000) { transport.published.await() }
                    watcher.scheduler.runCurrent()
                    // Before, the watcher failed it as lost along with the old link's requests.
                    assertNull(withTimeoutOrNull(300) { request.join() })
                    assertTrue(request.isActive)
                    request.cancel()
                }
            }
        } finally {
            // Let the watcher's coroutines finish here, so close() doesn't wait on a dispatcher no one runs.
            watcherScope.cancel()
            watcher.scheduler.runCurrent()
            session.close()
            client.disconnect()
        }
    }

    @Test
    fun silence_keeps_counting_while_the_phone_sleeps() = runTest {
        var nowMs = 1_000_000L
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
        val client = MqttClient(transport, nowMs = { nowMs })
        try {
            client.start(creds, clientId = "test")
            client.connected.first { it }
            assertEquals(0L, client.millisSinceLastMessage())
            // Ten minutes asleep: a monotonic clock stands still on a sleeping phone, the wall clock doesn't.
            nowMs += 600_000L
            assertEquals(600_000L, client.millisSinceLastMessage())
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun a_link_check_tells_alive_from_dead_from_unknown() = runTest {
        for ((answersPings, expected) in listOf(true to true, false to false, null to null)) {
            val client = MqttClient(DroppableTransport(answersPings))
            try {
                client.start(creds, clientId = "test")
                client.connected.first { it }
                // Real time: the timeout runs outside runTest's virtual clock.
                val result = withContext(Dispatchers.Default) { client.checkLink(timeoutMs = 100) }
                if (expected == null) assertNull(result, "answersPings=$answersPings")
                else assertEquals(expected, result, "answersPings=$answersPings")
            } finally {
                client.disconnect()
            }
        }
    }

    @Test
    fun a_link_check_that_times_out_stops_waiting_for_the_answer() = runTest {
        val transport = DroppableTransport(answersPings = false)
        val client = MqttClient(transport)
        try {
            client.start(creds, clientId = "test")
            client.connected.first { it }
            repeat(3) {
                // Real time: the timeout runs outside runTest's virtual clock.
                assertEquals(false, withContext(Dispatchers.Default) { client.checkLink(timeoutMs = 50) })
            }
            // Before, every unanswered ping left its listener on the client for good.
            assertEquals(0, transport.pendingPings)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun a_link_check_with_the_link_down_says_dead() = runTest {
        val client = MqttClient(DroppableTransport(answersPings = true))
        assertEquals(false, client.checkLink(timeoutMs = 100))
    }

    @Test
    fun a_credential_refresh_that_never_returns_does_not_stop_the_reconnecting() = runTest {
        val transport = RefusingTransport()
        val client = MqttClient(transport, initialBackoffMs = 10, credentialRefreshTimeoutMs = 100)
        var refreshes = 0
        client.credentialRefresher = { _, _ -> refreshes++; awaitCancellation() }
        try {
            client.start(creds, clientId = "test")
            // Real time: the supervisor runs on its own dispatcher, outside runTest's virtual clock.
            withContext(Dispatchers.Default) {
                // Three failures bring the refresh, which hangs; the attempts must go on regardless.
                withTimeout(5_000) { while (transport.opens < 5) delay(10) }
            }
            assertTrue(refreshes >= 1)
        } finally {
            client.disconnect()
        }
    }
}
