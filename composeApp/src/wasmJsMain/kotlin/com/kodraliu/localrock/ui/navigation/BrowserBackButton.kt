package com.kodraliu.localrock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
import kotlinx.browser.window
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event

private const val GUARD = "localrock-back-guard"

private fun isGuard(state: JsAny?): Boolean = (state as? JsString)?.toString() == GUARD

/** The page's history: guard entries keep the page's URL (`/`), so a reload never asks the Server for another path. */
private object WindowHistory : BrowserBackHistory {
    override fun pushGuard() = window.history.pushState(GUARD.toJsString(), "", null)
    override fun goBack() = window.history.back()
}

/**
 * Feeds the browser's back button into the same back dispatcher that the Escape key and the
 * App's system back gesture use, so it closes dialogs, leaves editors and pops screens alike.
 */
private class BrowserBackInput : NavigationEventInput() {
    private val back = BrowserBack(
        WindowHistory,
        startedOnGuard = isGuard(window.history.state),
        dispatchBack = { dispatchOnBackCompleted() },
    )
    private val onPopState: (Event) -> Unit = { event -> back.poppedTo(isGuard((event as PopStateEvent).state)) }

    init {
        window.addEventListener("popstate", onPopState)
    }

    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) = back.backAvailable(hasEnabledHandlers)

    fun dispose() = window.removeEventListener("popstate", onPopState)
}

/**
 * Makes the browser's back button work like the App's system back gesture. Compose only offers
 * its back dispatcher through an internal composition local; this is the one place that reads it.
 */
@OptIn(InternalComposeUiApi::class)
@Composable
fun BrowserBackButton() {
    val dispatcher = LocalCompatNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    DisposableEffect(dispatcher) {
        val input = BrowserBackInput()
        dispatcher.addInput(input)
        onDispose {
            dispatcher.removeInput(input)
            input.dispose()
        }
    }
}
