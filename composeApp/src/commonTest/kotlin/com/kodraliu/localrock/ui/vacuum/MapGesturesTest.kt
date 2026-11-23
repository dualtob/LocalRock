package com.kodraliu.localrock.ui.vacuum

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapGesturesTest {
    @Test
    fun two_fingers_dragged_together_up_or_down_tilt_the_camera() {
        assertTrue(isTiltDrag(2, Offset(0.5f, -9f), zoom = 1.002f, rotation = 0.3f))
        assertTrue(isTiltDrag(2, Offset(-1f, 7f), zoom = 1f, rotation = 0f))
    }

    @Test
    fun a_pinch_whose_midpoint_drifts_does_not_tilt() {
        assertFalse(isTiltDrag(2, Offset(0f, -9f), zoom = 1.05f, rotation = 0f))
        assertFalse(isTiltDrag(2, Offset(0f, -9f), zoom = 0.94f, rotation = 0f))
    }

    @Test
    fun a_twist_or_a_sideways_drag_or_one_finger_does_not_tilt() {
        assertFalse(isTiltDrag(2, Offset(0f, -9f), zoom = 1f, rotation = 4f))
        assertFalse(isTiltDrag(2, Offset(8f, -9f), zoom = 1f, rotation = 0f))
        assertFalse(isTiltDrag(1, Offset(0f, -9f), zoom = 1f, rotation = 0f))
    }

    @Test
    fun the_wheel_rolled_away_zooms_in_and_towards_the_user_zooms_out_by_the_same_step() {
        // In a browser Compose passes the wheel event's pixels on: 100 for one notch of a mouse wheel
        // (negative when rolled away), as Firefox and Chrome report it.
        assertEquals(1.2f, wheelZoomFactor(-100f), 0.0001f)
        assertEquals(1f / 1.2f, wheelZoomFactor(100f), 0.0001f)
        assertEquals(1f, wheelZoomFactor(0f))
    }

    @Test
    fun a_fast_flick_zooms_at_most_two_notches_at_a_time() {
        assertEquals(1.44f, wheelZoomFactor(-4000f), 0.0001f)
        assertEquals(1f / 1.44f, wheelZoomFactor(250f), 0.0001f)
    }

    @Test
    fun a_trackpad_scroll_of_a_few_pixels_zooms_a_little() {
        val f = wheelZoomFactor(-10f)
        assertTrue(f > 1f && f < 1.05f, "$f")
    }

    @Test
    fun on_a_drawing_map_the_right_or_middle_mouse_button_moves_the_map_and_the_left_one_draws() {
        assertTrue(dragMovesMap(PointerType.Mouse, primaryPressed = false))
        assertFalse(dragMovesMap(PointerType.Mouse, primaryPressed = true))
    }

    @Test
    fun a_finger_or_pen_never_counts_as_a_mouse_button_drag() {
        // Android reports no buttons for a touch, so the button state alone would mistake every finger for one.
        assertFalse(dragMovesMap(PointerType.Touch, primaryPressed = false))
        assertFalse(dragMovesMap(PointerType.Stylus, primaryPressed = false))
    }
}
