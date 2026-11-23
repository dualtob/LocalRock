package com.kodraliu.localrock.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SafeAreaTest {

    @Test
    fun css_pixels_become_device_pixels() {
        // iPhone 15 portrait: 59 px status bar / Dynamic Island, 34 px home indicator, at 3x.
        assertEquals(SafeArea(left = 0, top = 177, right = 0, bottom = 102), safeAreaFromCss("0px", "59px", "0px", "34px", density = 3f))
        // Landscape: the notch side and the home indicator.
        assertEquals(SafeArea(left = 177, top = 0, right = 177, bottom = 63), safeAreaFromCss("59px", "0px", "59px", "21px", density = 3f))
    }

    @Test
    fun fractional_insets_round_to_the_nearest_device_pixel() {
        assertEquals(SafeArea(left = 0, top = 141, right = 0, bottom = 0), safeAreaFromCss("0px", "47px", "0px", "0px", density = 3f))
        assertEquals(SafeArea(left = 1, top = 2, right = 0, bottom = 0), safeAreaFromCss("0.5px", "0.75px", "0px", "0px", density = 2f))
    }

    @Test
    fun a_desktop_browser_has_no_safe_area() {
        assertEquals(SafeArea.None, safeAreaFromCss("0px", "0px", "0px", "0px", density = 1f))
        // Nothing to read (an older browser, or the probe isn't laid out yet).
        assertEquals(SafeArea.None, safeAreaFromCss("", "", "", "", density = 2f))
        assertEquals(SafeArea.None, safeAreaFromCss("auto", "-4px", "nonsense", "NaNpx", density = 2f))
    }
}
