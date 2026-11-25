package com.kodraliu.localrock.shared.vacuum

import com.kodraliu.localrock.shared.vacuum.RobotHarness.Companion.robotTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Dock settings are what the dock said; one it never answered for stays unknown, and its rows aren't shown. */
class DockSettingsAnswersTest {

    @Test
    fun a_dock_setting_the_robot_does_not_answer_stays_unknown() = robotTest {
        answer("get_dust_collection_mode", json("""{"mode":2}"""))

        repository.fetchDockSettings()

        val dock = repository.dockSettings.value
        assertEquals(2, dock.autoEmptyMode)
        assertNull(dock.washMode)
        assertNull(dock.smartWash)
    }

    @Test
    fun dock_settings_keep_the_last_answer_when_a_read_fails_while_offline() = robotTest {
        answerDock()
        repository.fetchDockSettings()

        goOffline()
        repository.fetchDockSettings()

        val dock = repository.dockSettings.value
        assertEquals(1, dock.washMode)
        assertEquals(SmartWashSettings(mode = 0, intervalSeconds = 1200), dock.smartWash)
        assertEquals(2, dock.autoEmptyMode)
    }

    @Test
    fun dock_settings_keep_the_last_answer_when_the_robot_refuses_a_read() = robotTest {
        answerDock()
        repository.fetchDockSettings()

        refuse("get_wash_towel_mode")
        refuse("get_smart_wash_params")
        refuse("get_dust_collection_mode")
        repository.fetchDockSettings()

        val dock = repository.dockSettings.value
        assertEquals(1, dock.washMode)
        assertEquals(SmartWashSettings(mode = 0, intervalSeconds = 1200), dock.smartWash)
        assertEquals(2, dock.autoEmptyMode)
    }

    private fun RobotHarness.answerDock() {
        answer("get_wash_towel_mode", json("""{"wash_mode":1}"""))
        answer("get_smart_wash_params", json("""{"smart_wash":0,"wash_interval":1200}"""))
        answer("get_dust_collection_mode", json("""{"mode":2}"""))
    }

    private fun json(text: String) = Json.parseToJsonElement(text)
}
