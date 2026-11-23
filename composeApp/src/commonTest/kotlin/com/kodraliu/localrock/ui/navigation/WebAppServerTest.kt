package com.kodraliu.localrock.ui.navigation

import com.kodraliu.localrock.shared.settings.AppSettings
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/** On the web the Server is the page's own address, so there is nothing to set up before login. */
class WebAppServerTest {

    private val origin = "https://robots.lan:555"

    @Test
    fun the_page_origin_is_the_server() {
        val stored = MapSettings().apply { putString("server_base_url", "https://elsewhere.lan") }

        assertEquals(origin, AppSettings(stored, pageOrigin = origin).serverBaseUrl.value)
    }

    @Test
    fun the_server_cannot_be_changed_away_from_the_page_origin() {
        val settings = AppSettings(MapSettings(), pageOrigin = origin)

        settings.setServerBaseUrl("https://elsewhere.lan")
        settings.setServerBaseUrl(null)

        assertEquals(origin, settings.serverBaseUrl.value)
    }

    @Test
    fun the_web_app_opens_on_login_with_no_setup_step() {
        assertEquals(Login, firstScreen(LaunchState(isWebApp = true, introSeen = false, demoMode = false, serverUrl = origin, loggedIn = false)))
    }

    @Test
    fun the_web_app_opens_on_the_robot_list_when_logged_in() {
        assertEquals(DeviceList, firstScreen(LaunchState(isWebApp = true, introSeen = false, demoMode = false, serverUrl = origin, loggedIn = true)))
    }

    @Test
    fun the_app_still_starts_with_the_intro_then_server_setup() {
        assertEquals(Welcome, firstScreen(LaunchState(isWebApp = false, introSeen = false, demoMode = false, serverUrl = null, loggedIn = false)))
        assertEquals(ServerSetup, firstScreen(LaunchState(isWebApp = false, introSeen = true, demoMode = false, serverUrl = null, loggedIn = false)))
        assertEquals(Login, firstScreen(LaunchState(isWebApp = false, introSeen = true, demoMode = false, serverUrl = origin, loggedIn = false)))
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
