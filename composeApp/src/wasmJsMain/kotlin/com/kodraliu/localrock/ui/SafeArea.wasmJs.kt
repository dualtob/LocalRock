@file:OptIn(InternalComposeUiApi::class)

package com.kodraliu.localrock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.PlatformWindowInsetsProviderNode
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * Gives the App the phone's safe area as its system bars. Compose on the web reports no window
 * insets, so without this the App would draw its top bar under the notch and its bottom panels
 * under the home indicator. `index.html` lays out `#safe-area` with `env(safe-area-inset-*)` as
 * its padding; that is read again whenever the window resizes (rotation). On a computer it's 0.
 *
 * Compose reads insets in two places, so both get the safe area: `WindowInsets.systemBars` and the
 * like (Scaffold, top app bars) read [LocalPlatformWindowInsets], and the padding modifiers
 * (`safeDrawingPadding()`, `navigationBarsPadding()`) read the nearest ancestor
 * [PlatformWindowInsetsProviderNode]. Both are Compose internals for the platforms' own insets.
 */
@Composable
fun ProvideSafeArea(content: @Composable () -> Unit) {
    var safeArea by remember { mutableStateOf(readSafeArea()) }
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { safeArea = readSafeArea() }
        window.addEventListener("resize", listener)
        onDispose { window.removeEventListener("resize", listener) }
    }
    val safeAreaNow = remember { { safeArea } }
    val outer = LocalPlatformWindowInsets.current
    val insets = remember(outer) { SafeAreaWindowInsets(outer, safeAreaNow) }
    CompositionLocalProvider(LocalPlatformWindowInsets provides insets) {
        Box(SafeAreaInsetsElement(safeAreaNow)) { content() }
    }
}

private fun readSafeArea(): SafeArea {
    val probe = document.getElementById("safe-area") ?: return SafeArea.None
    val style = window.getComputedStyle(probe)
    return safeAreaFromCss(
        left = style.paddingLeft,
        top = style.paddingTop,
        right = style.paddingRight,
        bottom = style.paddingBottom,
        density = window.devicePixelRatio.toFloat(),
    )
}

/**
 * [base] (what Compose itself reports, such as the keyboard) plus the safe area as the system bars:
 * the status bar at the top, the home indicator at the bottom, and the notch on either side in
 * landscape. The insets are read at layout time, so a rotation only relays out.
 */
private class SafeAreaWindowInsets(
    private val base: PlatformWindowInsets,
    private val safeArea: () -> SafeArea,
) : PlatformWindowInsets by base {
    override val systemBars = base.systemBars.orAtLeast(
        left = { safeArea().left }, top = { safeArea().top }, right = { safeArea().right }, bottom = { safeArea().bottom },
    )
    override val statusBars = base.statusBars.orAtLeast(top = { safeArea().top })
    override val navigationBars = base.navigationBars.orAtLeast(bottom = { safeArea().bottom })
    override val displayCutout = base.displayCutout.orAtLeast(left = { safeArea().left }, right = { safeArea().right })

    override fun excluding(safeInsets: Boolean, ime: Boolean): PlatformWindowInsets =
        if (safeInsets) base.excluding(safeInsets = true, ime = ime)
        else SafeAreaWindowInsets(base.excluding(safeInsets = false, ime = ime), safeArea)
}

private fun PlatformInsets.orAtLeast(
    left: () -> Int = { 0 },
    top: () -> Int = { 0 },
    right: () -> Int = { 0 },
    bottom: () -> Int = { 0 },
) = PlatformInsets(
    getLeft = { maxOf(this.left, left()) },
    getTop = { maxOf(this.top, top()) },
    getRight = { maxOf(this.right, right()) },
    getBottom = { maxOf(this.bottom, bottom()) },
)

private data class SafeAreaInsetsElement(val safeArea: () -> SafeArea) : ModifierNodeElement<SafeAreaInsetsNode>() {
    override fun create() = SafeAreaInsetsNode(safeArea)
    override fun update(node: SafeAreaInsetsNode) = Unit
}

private class SafeAreaInsetsNode(private val safeArea: () -> SafeArea) : PlatformWindowInsetsProviderNode() {
    override fun calculatePlatformInsets(ancestorWindowInsets: PlatformWindowInsets): PlatformWindowInsets =
        SafeAreaWindowInsets(ancestorWindowInsets, safeArea)
}
