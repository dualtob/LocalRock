package com.kodraliu.localrock.shared.vacuum

import com.kodraliu.localrock.shared.mqtt.MqttClient
import com.kodraliu.localrock.shared.mqtt.MqttCreds
import com.kodraliu.localrock.shared.mqtt.MqttNativeConnection
import com.kodraliu.localrock.shared.mqtt.MqttNativeTransport
import com.kodraliu.localrock.shared.mqtt.MqttTopics
import com.kodraliu.localrock.shared.protocol.RoborockCodec
import com.kodraliu.localrock.shared.protocol.RoborockMessage
import com.kodraliu.localrock.shared.protocol.RoborockProtocol
import com.kodraliu.localrock.shared.protocol.V1Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.concurrent.Volatile

/**
 * A real [MqttClient], [VacuumSession] and [VacuumRepository] talking to a pretend Robot over a fake
 * MQTT transport. The Robot decodes each request with the session's local key and the same codec the
 * session uses, and answers from what the test told it with [answer] and [refuse]. A command it
 * wasn't told about is answered "unknown_method", as real firmware does.
 *
 * Use it through [robotTest]. Its clocks are real ones: the client and session time out in real time.
 */
class RobotHarness private constructor() {

    private val transport = RobotTransport()

    val mqtt = MqttClient(transport, initialBackoffMs = 10L)

    /** Where [repository] keeps the commands the Robot refused as unknown. */
    val unsupportedCommandStore = UnsupportedCommandStore.InMemory()

    val repository = newRepository()

    /**
     * A second repository for the same Robot on the same [unsupportedCommandStore], as the app builds
     * after a restart. It isn't started; read what it loaded, don't send through it.
     */
    fun repositoryOnSameStore(): VacuumRepository = newRepository()

    private fun newRepository() = VacuumRepository(
        duid = DUID,
        localKey = LOCAL_KEY,
        rriotKeyProvider = { "" },
        mqttClient = mqtt,
        topicsProvider = { TOPICS },
        nowEpochSeconds = { NOW },
        unsupportedCommandStore = unsupportedCommandStore,
    )

    val session: VacuumSession get() = repository.session

    private val replies = mutableMapOf<String, (JsonElement?) -> JsonObject>()

    /** The methods the Robot was asked, in order. */
    val requests: List<String> get() = transport.requests.filter { it != WARM_UP }

    /** The Robot answers [method] with [result]. */
    fun answer(method: String, result: JsonElement) {
        replies[method] = { buildJsonObject { put("result", result) } }
    }

    /** The Robot answers [method] with an error, the way it refuses a command it can't do now. */
    fun refuse(method: String, code: Int = -10007, message: String = "Invalid status") {
        replies[method] = {
            buildJsonObject {
                put("error", buildJsonObject {
                    put("code", code)
                    put("message", message)
                })
            }
        }
    }

    /** The link drops and can't be made again until [comeBack]: every request fails at once. */
    suspend fun goOffline() {
        transport.reachable = false
        transport.dropLink()
        withTimeout(WAIT_MS) { mqtt.connected.first { !it } }
    }

    suspend fun comeBack() {
        transport.reachable = true
        mqtt.retryNow()
        withTimeout(WAIT_MS) { mqtt.connected.first { it } }
    }

    private suspend fun connect() {
        mqtt.start(MqttCreds(host = "broker.example", port = 8883, tls = true, username = "u", password = "p"), clientId = "test")
        withTimeout(WAIT_MS) { mqtt.connected.first { it } }
        session.start()
        // The session listens from a coroutine of its own, and a reply sent before it has started
        // listening is lost. Ask until one gets through, so a test's first request isn't the one lost.
        withTimeout(WAIT_MS) {
            // The request's own timeout is the longer one: its TimeoutCancellationException would
            // not be turned into null here, and would end the wait.
            while (withTimeoutOrNull(WARM_UP_TRY_MS) { session.sendCommand(WARM_UP, timeoutMs = WAIT_MS) } == null) Unit
        }
    }

    private suspend fun close() {
        repository.detach()
        session.close()
        mqtt.disconnect()
    }

    /** What the Robot sends back for one request frame, or null when it has nothing to say. */
    private fun replyTo(frame: ByteArray): ByteArray? {
        val message = RoborockCodec(LOCAL_KEY, prefixed = false).decode(frame).singleOrNull() ?: return null
        val outer = Json.parseToJsonElement(message.payload.decodeToString()).jsonObject
        val inner = Json.parseToJsonElement(
            outer["dps"]!!.jsonObject[V1Envelope.DPS_REQUEST]!!.jsonPrimitive.content,
        ).jsonObject
        val id = inner["id"]!!.jsonPrimitive.int
        val method = inner["method"]!!.jsonPrimitive.content
        transport.requests += method
        val body = replies[method]?.invoke(inner["params"])
            ?: buildJsonObject { put("result", UNKNOWN_METHOD_RESULT) }
        val reply = JsonObject(mapOf("id" to JsonPrimitive(id)) + body)
        val payload = buildJsonObject {
            put("dps", buildJsonObject { put(V1Envelope.DPS_RESPONSE, reply.toString()) })
            put("t", message.timestamp)
        }.toString().encodeToByteArray()
        return RoborockCodec(LOCAL_KEY, prefixed = false).encode(
            RoborockMessage(
                protocol = RoborockProtocol.RPC_RESPONSE,
                payload = payload,
                seq = message.seq,
                random = message.random,
                timestamp = message.timestamp,
            ),
        )
    }

    private inner class RobotTransport : MqttNativeTransport {
        @Volatile var reachable = true
        val requests = mutableListOf<String>()
        private var link: Link? = null

        fun dropLink() {
            link?.let {
                it.isConnected = false
                it.onDisconnected()
            }
        }

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
            if (!reachable) throw RuntimeException("Network is unreachable")
            val opened = Link(onMessage, onDisconnected)
            link = opened
            onConnected()
            return opened
        }

        private inner class Link(
            val onMessage: (topic: String, payload: ByteArray) -> Unit,
            val onDisconnected: () -> Unit,
        ) : MqttNativeConnection {
            @Volatile override var isConnected = true
            override fun subscribe(topic: String, qos: Int) {}
            override fun unsubscribe(topic: String) {}
            override fun close() { isConnected = false }

            override fun publish(topic: String, payload: ByteArray, qos: Int, retain: Boolean) {
                check(isConnected) { "Link is down" }
                if (topic != TOPICS.publishTopic) return
                replyTo(payload)?.let { onMessage(TOPICS.subscribeTopic, it) }
            }
        }
    }

    companion object {
        const val DUID = "test-duid"
        const val LOCAL_KEY = "0123456789abcdef"
        const val NOW = 1_764_000_000
        val TOPICS = MqttTopics(publishTopic = "rr/m/i/u/user/$DUID", subscribeTopic = "rr/m/o/u/user/$DUID")
        private const val WARM_UP = "harness_warm_up"
        private const val WARM_UP_TRY_MS = 200L
        private const val WAIT_MS = 5_000L

        /**
         * Runs [block] against a connected harness, on [Dispatchers.Default] so that request timeouts
         * run in real time rather than in [runTest]'s skipped-ahead virtual time, which would fire them
         * before a reply from another thread could arrive.
         */
        fun robotTest(block: suspend RobotHarness.() -> Unit): TestResult = runTest {
            withContext(Dispatchers.Default) {
                val harness = RobotHarness()
                try {
                    harness.connect()
                    harness.block()
                } finally {
                    harness.close()
                }
            }
        }
    }
}
