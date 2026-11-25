package com.kodraliu.localrock.shared.vacuum

import kotlin.test.Test
import kotlin.test.assertEquals

/** The map poll asks again soon after any failure, not only before the first map arrives. */
class MapPollDelayTest {

    @Test
    fun after_a_failed_request_the_map_is_asked_for_again_soon() {
        assertEquals(5_000L, mapPollDelayMs(lastFailed = true, robotBusy = false))
        assertEquals(5_000L, mapPollDelayMs(lastFailed = true, robotBusy = true))
    }

    @Test
    fun after_a_map_arrived_a_busy_robot_is_asked_often_and_an_idle_one_rarely() {
        assertEquals(8_000L, mapPollDelayMs(lastFailed = false, robotBusy = true))
        assertEquals(60_000L, mapPollDelayMs(lastFailed = false, robotBusy = false))
    }
}
