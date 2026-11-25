package com.kodraliu.localrock.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Browsers have no dynamic colour, so the standard LocalRock scheme is used. */
@Composable
actual fun platformDynamicColorScheme(useDark: Boolean): ColorScheme? = null

/**
 * The browser's dark mode, kept current. Compose's own `isSystemInDarkTheme()` only listens for the
 * media query's change event, which iOS doesn't send to a home-screen Web App that was in the
 * background when the phone switched (WebKit bugs 262212, 266853). So the value is also read again
 * whenever the page becomes visible, is shown from the back-forward cache, or gets focus.
 */
@Composable
actual fun systemInDarkTheme(): Boolean {
    var dark by remember { mutableStateOf(prefersDark()) }
    DisposableEffect(Unit) {
        val stop = watchColorScheme { dark = prefersDark() }
        onDispose { stopWatching(stop) }
    }
    return dark
}

/**
 * The page around the App follows the App's theme, not only the system's: the strip behind the
 * status bar is `#app`'s background, and the theme-color meta tags colour the browser's own bars.
 * Without this, the Dark setting on a light phone left a light strip above a dark App.
 */
@Composable
actual fun SystemBarsAppearance(darkTheme: Boolean) {
    SideEffect { applyPageTheme(darkTheme) }
}

private fun prefersDark(): Boolean = js("window.matchMedia('(prefers-color-scheme: dark)').matches")

/** Calls [onChange] on every hint that dark mode may have changed; returns what [stopWatching] needs. */
private fun watchColorScheme(onChange: () -> Unit): JsAny = js(
    """{
        const media = window.matchMedia('(prefers-color-scheme: dark)');
        const check = () => onChange();
        const onVisible = () => { if (document.visibilityState === 'visible') onChange(); };
        if (media.addEventListener) media.addEventListener('change', check); else media.addListener(check);
        document.addEventListener('visibilitychange', onVisible);
        window.addEventListener('pageshow', check);
        window.addEventListener('focus', check);
        return () => {
            if (media.removeEventListener) media.removeEventListener('change', check); else media.removeListener(check);
            document.removeEventListener('visibilitychange', onVisible);
            window.removeEventListener('pageshow', check);
            window.removeEventListener('focus', check);
        };
    }"""
)

private fun stopWatching(stop: JsAny): Unit = js("{ stop(); }")

/** Sets `data-theme` on `<html>` (see index.html) and points both theme-color tags at the App's colour. */
private fun applyPageTheme(dark: Boolean): Unit = js(
    """{
        const root = document.documentElement;
        root.dataset.theme = dark ? 'dark' : 'light';
        const colour = dark ? '#141218' : '#fef7ff';
        document.querySelectorAll('meta[name="theme-color"]').forEach((m) => m.setAttribute('content', colour));
    }"""
)
