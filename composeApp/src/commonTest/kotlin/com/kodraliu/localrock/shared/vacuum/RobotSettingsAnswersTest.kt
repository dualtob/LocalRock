package com.kodraliu.localrock.shared.vacuum

import com.kodraliu.localrock.shared.vacuum.RobotHarness.Companion.robotTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Robot settings are what the Robot said, never a made-up default. */
class RobotSettingsAnswersTest {

    @Test
    fun settings_are_unknown_before_the_robot_is_asked() = robotTest {
        assertEquals(RobotSettings(volume = null, childLock = null, ledEnabled = null, carpetMode = null), repository.robotSettings.value)
    }

    @Test
    fun a_setting_the_robot_does_not_answer_stays_unknown() = robotTest {
        // Only the volume is answered; the rest get "unknown_method", as from a Robot without them.
        answer(GET_SOUND_VOLUME, json("[45]"))

        repository.fetchRobotSettings()

        val settings = repository.robotSettings.value
        assertEquals(45, settings.volume)
        assertNull(settings.childLock)
        // The LED used to show "on" for a Robot that never said so.
        assertNull(settings.ledEnabled)
        assertNull(settings.carpetMode)
    }

    @Test
    fun answered_settings_are_shown_as_the_robot_gave_them() = robotTest {
        answerAllSettings()

        repository.fetchRobotSettings()

        assertEquals(RobotSettings(volume = 45, childLock = true, ledEnabled = false, carpetMode = 2), repository.robotSettings.value)
    }

    @Test
    fun settings_keep_the_last_answer_when_a_read_fails_while_offline() = robotTest {
        answerAllSettings()
        repository.fetchRobotSettings()

        goOffline()
        repository.fetchRobotSettings()

        assertEquals(RobotSettings(volume = 45, childLock = true, ledEnabled = false, carpetMode = 2), repository.robotSettings.value)
    }

    @Test
    fun settings_keep_the_last_answer_when_the_robot_refuses_a_read() = robotTest {
        answerAllSettings()
        repository.fetchRobotSettings()

        refuse(GET_SOUND_VOLUME)
        refuse(GET_CHILD_LOCK_STATUS)
        refuse("get_led_status")
        refuse("get_carpet_clean_mode")
        repository.fetchRobotSettings()

        assertEquals(RobotSettings(volume = 45, childLock = true, ledEnabled = false, carpetMode = 2), repository.robotSettings.value)
    }

    private fun RobotHarness.answerAllSettings() {
        // The shapes a Qrevo answers in.
        answer(GET_SOUND_VOLUME, json("[45]"))
        answer(GET_CHILD_LOCK_STATUS, json("""{"lock_status":1}"""))
        answer("get_led_status", json("[0]"))
        answer("get_carpet_clean_mode", json("""[{"carpet_clean_mode":2}]"""))
    }

    private fun json(text: String) = Json.parseToJsonElement(text)
}
