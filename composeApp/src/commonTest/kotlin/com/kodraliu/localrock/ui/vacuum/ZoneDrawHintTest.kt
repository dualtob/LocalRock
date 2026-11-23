package com.kodraliu.localrock.ui.vacuum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZoneDrawHintTest {
    @Test
    fun the_web_app_explains_the_mouse() {
        val hint = zoneDrawHint(isWebApp = true)
        assertTrue("Scroll to zoom" in hint && "Right-drag to move" in hint, hint)
        assertFalse("fingers" in hint, hint)
    }

    @Test
    fun the_app_keeps_its_touch_hint() {
        assertEquals(
            "Drag to draw a zone · Two fingers to zoom and move · Double-tap to reset the view",
            zoneDrawHint(isWebApp = false),
        )
    }
}
