package com.kodraliu.localrock

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.kodraliu.localrock.shared.AppContainer
import com.kodraliu.localrock.shared.mqtt.UnavailableMqttTransport
import com.kodraliu.localrock.shared.mqtt.defaultMqttTransport
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * Web App entry point: the App's root composable, filling the page. Login lives in localStorage,
 * and the Server is the origin the page was loaded from.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val container = AppContainer(
        StorageSettings(),
        defaultMqttTransport() ?: UnavailableMqttTransport,
        pageOrigin = window.location.origin,
    )
    ComposeViewport(document.body!!) {
        App(container)
    }
}
