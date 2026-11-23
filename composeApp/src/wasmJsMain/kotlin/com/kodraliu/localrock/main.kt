package com.kodraliu.localrock

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.kodraliu.localrock.shared.AppContainer
import com.kodraliu.localrock.shared.mqtt.webAppMqttTransport
import com.kodraliu.localrock.ui.ProvideSafeArea
import com.kodraliu.localrock.ui.navigation.BrowserBackButton
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * Web App entry point: the App's root composable, filling `#app` (the visible part of the page,
 * see `index.html`) with the phone's safe area as its system bars. Login lives in localStorage,
 * and the Server is the origin the page was loaded from.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val pageOrigin = window.location.origin
    val container = AppContainer(
        StorageSettings(),
        webAppMqttTransport(pageOrigin),
        pageOrigin = pageOrigin,
    )
    ComposeViewport(document.getElementById("app")!!) {
        BrowserBackButton()
        ProvideSafeArea { App(container) }
    }
}
