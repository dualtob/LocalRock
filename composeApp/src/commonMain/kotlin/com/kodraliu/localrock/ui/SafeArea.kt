package com.kodraliu.localrock.ui

import kotlin.math.roundToInt

/**
 * The edges of a phone screen the Web App draws behind but keeps content clear of (the notch or
 * Dynamic Island, the status bar, the home indicator), in device pixels. The browser reports them
 * as CSS `env(safe-area-inset-*)`; the Web App passes them to Compose as the system bars, so the
 * App's edge-to-edge handling (Scaffold, top bars, bottom panels) applies them as on a phone.
 */
data class SafeArea(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    companion object {
        val None = SafeArea(0, 0, 0, 0)
    }
}

/**
 * [SafeArea] from the computed CSS lengths of the four insets (like "47px"), at [density] device
 * pixels per CSS pixel. Anything that isn't a non-negative pixel length counts as no inset.
 */
fun safeAreaFromCss(left: String, top: String, right: String, bottom: String, density: Float): SafeArea {
    fun px(css: String): Int {
        val value = css.trim().removeSuffix("px").toFloatOrNull() ?: return 0
        return if (value.isFinite() && value > 0f) (value * density).roundToInt() else 0
    }
    return SafeArea(px(left), px(top), px(right), px(bottom))
}
