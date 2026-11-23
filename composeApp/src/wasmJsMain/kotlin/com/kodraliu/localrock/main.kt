package com.kodraliu.localrock

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.kodraliu.localrock.shared.AppContainer
import com.kodraliu.localrock.shared.mqtt.webAppMqttTransport
import com.kodraliu.localrock.shared.settings.webAppServer
import com.kodraliu.localrock.ui.NoServerScreen
import com.kodraliu.localrock.ui.ProvideSafeArea
import com.kodraliu.localrock.ui.navigation.BrowserBackButton
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Web App entry point: the App's root composable, filling `#app` (the visible part of the page,
 * see `index.html`) with the phone's safe area as its system bars. Login lives in localStorage.
 * The Server is whatever the deployment names in `config.json` next to `index.html` (ADR 0002);
 * without one, the Web App only says so.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MainScope().launch {
        val container = webAppServer(fetchConfigJson())?.let { server ->
            AppContainer(StorageSettings(), webAppMqttTransport(server), fixedServer = server)
        }
        ComposeViewport(document.getElementById("app")!!) {
            BrowserBackButton()
            ProvideSafeArea {
                if (container != null) App(container) else NoServerScreen()
            }
        }
    }
}
