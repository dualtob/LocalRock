package com.kodraliu.localrock.ui.navigation

import com.kodraliu.localrock.shared.settings.AppSettings
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Web App's Server comes from its deployment (`config.json`, see ADR 0002), so there is
 * nothing to set up before login. The App keeps its own stored Server.
 */
class WebAppServerTest {

    private val server = "https://robots.lan:555"

    @Test
    fun the_deployments_server_wins_over_a_stored_one() {
        val stored = MapSettings().apply { putString("server_base_url", "https://elsewhere.lan") }

        assertEquals(server, AppSettings(stored, fixedServer = server).serverBaseUrl.value)
    }

    @Test
    fun the_deployments_server_cannot_be_changed_or_cleared() {
        val settings = AppSettings(MapSettings(), fixedServer = server)

        settings.setServerBaseUrl("https://elsewhere.lan")
        settings.setServerBaseUrl(null)

        assertEquals(server, settings.serverBaseUrl.value)
    }

    @Test
    fun the_web_app_opens_on_login_with_no_setup_step() {
        assertEquals(Login, firstScreen(LaunchState(isWebApp = true, introSeen = false, demoMode = false, serverUrl = server, loggedIn = false)))
    }

    @Test
    fun the_web_app_opens_on_the_robot_list_when_logged_in() {
        assertEquals(DeviceList, firstScreen(LaunchState(isWebApp = true, introSeen = false, demoMode = false, serverUrl = server, loggedIn = true)))
    }

    @Test
    fun the_app_still_starts_with_the_intro_then_server_setup() {
        assertEquals(Welcome, firstScreen(LaunchState(isWebApp = false, introSeen = false, demoMode = false, serverUrl = null, loggedIn = false)))
        assertEquals(ServerSetup, firstScreen(LaunchState(isWebApp = false, introSeen = true, demoMode = false, serverUrl = null, loggedIn = false)))
        assertEquals(Login, firstScreen(LaunchState(isWebApp = false, introSeen = true, demoMode = false, serverUrl = server, loggedIn = false)))
        assertEquals(DeviceList, firstScreen(LaunchState(isWebApp = false, introSeen = true, demoMode = true, serverUrl = null, loggedIn = false)))
    }

    @Test
    fun the_app_keeps_its_stored_server() {
        val stored = MapSettings().apply { putString("server_base_url", "https://elsewhere.lan") }
        val settings = AppSettings(stored)

        assertEquals("https://elsewhere.lan", settings.serverBaseUrl.value)
        settings.setServerBaseUrl("https://other.lan")
        assertEquals("https://other.lan", settings.serverBaseUrl.value)
    }
}
