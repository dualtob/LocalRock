package com.kodraliu.localrock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kodraliu.localrock.shared.AppContainer
import kotlinx.coroutines.launch
import com.kodraliu.localrock.shared.settings.ThemeMode
import com.kodraliu.localrock.ui.FrameRateOverlay
import com.kodraliu.localrock.ui.LocalAppContainer
import com.kodraliu.localrock.ui.SystemBarsAppearance
import com.kodraliu.localrock.ui.adaptive.ProvideWindowLayout
import com.kodraliu.localrock.ui.appColorScheme
import com.kodraliu.localrock.ui.systemInDarkTheme
import com.kodraliu.localrock.ui.messages.MessageBannerArea
import com.kodraliu.localrock.ui.messages.TransientMessageHost
import com.kodraliu.localrock.ui.navigation.AppNavHost

@Composable
fun App(container: AppContainer) {
    val themeMode by container.appSettings.themeMode.collectAsState()
    val showFrameRate by container.appSettings.showFrameRate.collectAsState()
    val systemDark = systemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val colorScheme = appColorScheme(useDark)
    SystemBarsAppearance(useDark)

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner, container) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) container.setForeground(false)
            if (event == Lifecycle.Event.ON_START) {
                container.setForeground(true)
                scope.launch {
                    container.onEnterForeground()
                    // A robot can drop off while the app is away, and the list is not reloaded on return.
                    // Only after the link check: the probe renews a silent link too, and two renewals
                    // at once orphaned the requests of the first.
                    container.deviceRepository.probeReachability()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalAppContainer provides container) {
        MaterialTheme(colorScheme = colorScheme) {
            ProvideWindowLayout {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            MessageBannerArea(container.messages)
                            Box(Modifier.weight(1f)) {
                                AppNavHost()
                                TransientMessageHost(container.messages)
                            }
                        }
                        if (container.isWebApp && showFrameRate) {
                            FrameRateOverlay(Modifier.align(Alignment.BottomStart).safeDrawingPadding())
                        }
                    }
                }
            }
        }
    }
}
