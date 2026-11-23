package com.kodraliu.localrock

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.kodraliu.localrock.shared.AppContainer
import com.kodraliu.localrock.shared.mqtt.webAppMqttTransport
import com.kodraliu.localrock.shared.settings.WebAppServer
import com.kodraliu.localrock.shared.settings.webAppServer
import com.kodraliu.localrock.ui.NoServerScreen
import com.kodraliu.localrock.ui.ProvideSafeArea
import com.kodraliu.localrock.ui.navigation.BrowserBackButton
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.fetch.NO_CACHE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response

/**
 * Web App entry point: the App's root composable, filling `#app` (the visible part of the page,
 * see `index.html`) with the phone's safe area as its system bars. Login lives in localStorage.
 * The Server is whatever the deployment names in `config.json` next to `index.html` (ADR 0002);
 * without one, the Web App only says so.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MainScope().launch {
        val server = webAppServer(fetchConfigJson())
        val container = (server as? WebAppServer.Set)?.let {
            AppContainer(StorageSettings(), webAppMqttTransport(it.url), fixedServer = it.url)
        }
        ComposeViewport(document.getElementById("app")!!) {
            BrowserBackButton()
            ProvideSafeArea {
                if (container != null) App(container) else NoServerScreen()
            }
        }
    }
}

/** The text of `config.json` beside the page, or null when it can't be fetched. Never cached. */
private suspend fun fetchConfigJson(): String? = try {
    val response = window.fetch("config.json", RequestInit(cache = RequestCache.NO_CACHE)).await<Response>()
    if (response.ok) response.text().await<JsString>().toString() else null
} catch (e: Throwable) {
    null
}
