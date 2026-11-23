package com.kodraliu.localrock.ui.vacuum

import kotlin.test.Test
import kotlin.test.assertEquals

class TypedClockNumberTest {
    @Test
    fun two_digits_typed_in_a_row_make_the_number() {
        val first = typedClockNumber(previous = 8, digit = 1, min = 0, max = 23, continuing = false)
        assertEquals(1, first)
        assertEquals(14, typedClockNumber(previous = first, digit = 4, min = 0, max = 23, continuing = true))
    }

    @Test
    fun a_digit_that_would_go_past_the_range_starts_a_new_number() {
        assertEquals(5, typedClockNumber(previous = 2, digit = 5, min = 0, max = 23, continuing = true))
        assertEquals(7, typedClockNumber(previous = 45, digit = 7, min = 0, max = 59, continuing = true))
    }

    @Test
    fun a_fresh_digit_replaces_the_number_and_one_outside_the_range_is_ignored() {
        assertEquals(3, typedClockNumber(previous = 22, digit = 3, min = 0, max = 23, continuing = false))
        // A 12-hour face runs 1 to 12, so 0 alone is not an hour.
        assertEquals(9, typedClockNumber(previous = 9, digit = 0, min = 1, max = 12, continuing = false))
        assertEquals(10, typedClockNumber(previous = 1, digit = 0, min = 1, max = 12, continuing = true))
    }
}
