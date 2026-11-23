package com.kodraliu.localrock.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Browsers have no dynamic colour, so the standard LocalRock scheme is used. */
@Composable
actual fun platformDynamicColorScheme(useDark: Boolean): ColorScheme? = null

/** A browser tab has no system bars to style. */
@Composable
actual fun SystemBarsAppearance(darkTheme: Boolean) = Unit
