package com.routeforge.routing.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CoordinateInputTest {
    @Test
    fun `a number with a decimal point is read as typed`() {
        assertEquals(-23.55, "-23.55".toCoordinateOrNull())
        assertEquals(10.0, "10".toCoordinateOrNull())
    }

    @Test
    fun `a decimal comma is read as a decimal point`() {
        assertEquals(-23.55, "-23,55".toCoordinateOrNull())
    }

    @Test
    fun `spaces around the number are ignored`() {
        assertEquals(6.5, " 6,5 ".toCoordinateOrNull())
    }

    @Test
    fun `text that is not a number gives nothing`() {
        assertNull("".toCoordinateOrNull())
        assertNull("abc".toCoordinateOrNull())
        assertNull("1,2,3".toCoordinateOrNull())
        assertNull("1.2.3".toCoordinateOrNull())
    }
}
