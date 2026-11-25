package com.kodraliu.localrock.shared.vacuum

import com.kodraliu.localrock.shared.vacuum.RobotHarness.Companion.robotTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The live map arrives, or the repository clearly says it didn't; a fresh request never queues behind a stuck one. */
class MapLoadTest {

    private val map = "a live map".encodeToByteArray()

    @Test
    fun a_map_the_robot_sends_arrives_and_the_load_ends_idle() = robotTest {
        answerMap(map)

        val response = repository.fetchMap()

        assertContentEquals(map, response.data)
        assertContentEquals(map, repository.mapBytes.value)
        assertEquals(MapLoad.IDLE, repository.mapLoad.value)
        assertEquals(1, mapRequests)
    }

    @Test
    fun an_unanswered_map_request_is_sent_once_more_then_fails() = robotTest {
        withholdMap()

        supervisorScope {
            val fetch = async { repository.fetchMap() }
            awaitMapLoad(MapLoad.LOADING)
            assertFails(fetch)
        }

        assertEquals(2, mapRequests)
        assertEquals(MapLoad.FAILED, repository.mapLoad.value)
    }

    @Test
    fun a_map_lost_once_arrives_on_the_second_request() = robotTest(mapTimeoutMs = RETRY_MAP_TIMEOUT_MS) {
        withholdMap()

        supervisorScope {
            val fetch = async { repository.fetchMap() }
            awaitMapRequests(1)
            answerMap(map)
            assertContentEquals(map, fetch.await().data)
        }

        assertEquals(2, mapRequests)
        assertEquals(MapLoad.IDLE, repository.mapLoad.value)
    }

    @Test
    fun a_map_request_cut_off_by_a_dropped_link_is_asked_again_on_the_new_link() = robotTest(mapTimeoutMs = LONG_MAP_TIMEOUT_MS) {
        withholdMap()

        supervisorScope {
            val fetch = async { repository.fetchMap() }
            awaitMapRequests(1)
            answerMap(map)
            goOffline()
            comeBack()
            // Well before the request's own timeout: the dropped link ended it, not the clock.
            val response = withTimeout(LONG_MAP_TIMEOUT_MS / 2) { fetch.await() }
            assertContentEquals(map, response.data)
        }

        assertEquals(2, mapRequests)
        assertEquals(MapLoad.IDLE, repository.mapLoad.value)
    }

    @Test
    fun with_the_link_still_down_the_second_map_request_waits_only_briefly() =
        robotTest(mapTimeoutMs = LONG_MAP_TIMEOUT_MS, mapRelinkWaitMs = SHORT_RELINK_WAIT_MS) {
            withholdMap()

            supervisorScope {
                val fetch = async { repository.fetchMap() }
                awaitMapRequests(1)
                goOffline()
                // The link stays down. Before, the wait for it took the whole map timeout.
                withTimeout(LONG_MAP_TIMEOUT_MS / 2) { assertFails(fetch) }
            }

            assertEquals(MapLoad.FAILED, repository.mapLoad.value)
        }

    @Test
    fun a_restart_gives_up_the_request_on_its_way_and_asks_afresh() = robotTest(mapTimeoutMs = LONG_MAP_TIMEOUT_MS) {
        withholdMap()

        supervisorScope {
            val first = async { repository.fetchMap() }
            awaitMapRequests(1)
            answerMap(map)

            val fresh = withTimeout(LONG_MAP_TIMEOUT_MS / 2) { repository.fetchMap(restart = true) }

            assertContentEquals(map, fresh.data)
            // Given up quietly: cancelled, not failed with an error to show.
            assertFailsWith<CancellationException> { first.await() }
        }

        assertEquals(2, mapRequests)
        assertEquals(MapLoad.IDLE, repository.mapLoad.value)
    }

    @Test
    fun a_detach_while_the_map_is_on_its_way_leaves_the_load_idle() = robotTest(mapTimeoutMs = LONG_MAP_TIMEOUT_MS) {
        withholdMap()

        supervisorScope {
            val fetch = async { repository.fetchMap() }
            awaitMapRequests(1)
            awaitMapLoad(MapLoad.LOADING)

            repository.detach()

            // Well before the request's own timeout: the detach ended it. Before, it stayed LOADING.
            withTimeout(LONG_MAP_TIMEOUT_MS / 2) { repository.mapLoad.first { it == MapLoad.IDLE } }
            assertFailsWith<CancellationException> { fetch.await() }
        }

        assertEquals(1, mapRequests)
    }

    @Test
    fun a_second_request_without_restart_waits_for_the_one_on_its_way() = robotTest(mapTimeoutMs = RETRY_MAP_TIMEOUT_MS) {
        withholdMap()

        supervisorScope {
            val first = async { repository.fetchMap() }
            awaitMapRequests(1)
            val second = async { repository.fetchMap() }
            answerMap(map)
            // The second sends no request of its own: both get the map from the first one's retry.
            assertContentEquals(map, first.await().data)
            assertContentEquals(map, second.await().data)
        }

        assertEquals(2, mapRequests)
    }

    private suspend fun RobotHarness.awaitMapLoad(state: MapLoad) {
        withTimeout(WAIT_MS) { repository.mapLoad.first { it == state } }
    }

    private suspend fun assertFails(fetch: kotlinx.coroutines.Deferred<MapResponse>) {
        val failure = runCatching { fetch.await() }.exceptionOrNull()
        check(failure != null && failure !is CancellationException) { "Expected the map request to fail, got $failure" }
    }

    private companion object {
        /** Long enough for a test to set the Robot up before the withheld request is retried. */
        const val RETRY_MAP_TIMEOUT_MS = 1_000L
        /** Longer than any test step: the request on its way can't time out meanwhile. */
        const val LONG_MAP_TIMEOUT_MS = 5_000L
        /** Well below [LONG_MAP_TIMEOUT_MS]. */
        const val SHORT_RELINK_WAIT_MS = 300L
        const val WAIT_MS = 5_000L
    }
}
