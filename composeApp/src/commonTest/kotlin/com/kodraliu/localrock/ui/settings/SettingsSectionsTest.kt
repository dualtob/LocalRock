package com.kodraliu.localrock.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsSectionsTest {

    @Test
    fun the_web_app_has_no_server_field_and_no_alerts() {
        // The Server is the page's origin, and a browser tab posts no alerts.
        assertEquals(
            listOf(SettingsSection.Appearance, SettingsSection.Contribute, SettingsSection.About),
            settingsSections(isWebApp = true, signedIn = true),
        )
    }

    @Test
    fun the_app_shows_its_server_field_and_alerts_once_signed_in() {
        assertEquals(
            listOf(SettingsSection.Appearance, SettingsSection.Connection, SettingsSection.Alerts, SettingsSection.Contribute, SettingsSection.About),
            settingsSections(isWebApp = false, signedIn = true),
        )
        assertEquals(
            listOf(SettingsSection.Appearance, SettingsSection.Connection, SettingsSection.Contribute, SettingsSection.About),
            settingsSections(isWebApp = false, signedIn = false),
        )
    }
}
