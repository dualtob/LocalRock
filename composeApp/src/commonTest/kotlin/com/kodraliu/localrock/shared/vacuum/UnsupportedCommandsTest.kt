package com.kodraliu.localrock.shared.vacuum

import com.kodraliu.localrock.shared.vacuum.RobotHarness.Companion.robotTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * An action with no read command to ask first is hidden once the Robot answers it with
 * unknown_method, and stays hidden after a restart. Only that answer counts.
 */
class UnsupportedCommandsTest {

    @Test
    fun a_command_the_robot_answers_with_unknown_method_is_remembered() = robotTest {
        // The harness's Robot answers anything it wasn't told about with unknown_method.
        assertFailsWith<RobotUnsupportedCommandException> { repository.startRemoteControl() }

        assertEquals(setOf("app_rc_start"), repository.unsupportedCommands.value)
    }

    @Test
    fun a_remembered_command_survives_a_new_repository_built_with_the_same_store() = robotTest {
        assertFailsWith<RobotUnsupportedCommandException> { repository.startRemoteControl() }

        val afterRestart = repositoryOnSameStore()

        assertEquals(setOf("app_rc_start"), afterRestart.unsupportedCommands.value)
    }

    @Test
    fun a_param_shape_error_is_not_remembered() = robotTest {
        refuse("app_rc_start", code = -10005, message = "invalid params")

        assertFailsWith<RobotCommandException> { repository.startRemoteControl() }

        assertEquals(emptySet(), repository.unsupportedCommands.value)
        assertEquals(emptySet(), repositoryOnSameStore().unsupportedCommands.value)
    }

    @Test
    fun a_beta_option_the_robot_does_not_know_hides_alone_though_another_shares_its_write_command() = robotTest {
        val first = BetaFeatureDef("first", "First", "", bit = null, getMethod = "get_first", setMethod = "set_shared")
        val second = BetaFeatureDef("second", "Second", "", bit = null, getMethod = "get_second", setMethod = "set_shared")
        val defs = listOf(first, second)
        answer("get_first", JsonPrimitive(1))
        answer("get_second", JsonPrimitive(0))
        repository.fetchBetaFeatures(defs)
        assertEquals(listOf("first", "second"), repository.betaFeatures.value.map { it.def.id })

        // set_shared isn't set up, so the Robot answers it with unknown_method.
        assertFailsWith<RobotUnsupportedCommandException> { repository.setBetaFeature("first", on = false) }

        assertEquals(listOf("second"), repository.betaFeatures.value.map { it.def.id })
        repository.fetchBetaFeatures(defs)
        assertEquals(listOf("second"), repository.betaFeatures.value.map { it.def.id })
    }
}
