package com.routeforge.designsystem.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class CoordinateFormatTest {
    @Test
    fun `formats with a dot decimal separator regardless of the default locale`() {
        val previousDefault = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY) // uses a comma decimal separator
            assertEquals("40.71234, -74.00123", formatCoordinates(40.71234, -74.00123))
        } finally {
            Locale.setDefault(previousDefault)
        }
    }
}
