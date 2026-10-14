package com.routeforge.coredata

import com.routeforge.coredomain.model.TourSection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InitialSeenSectionsTest {
    @Test
    fun `an already-authorized install starts with every section already seen`() {
        assertEquals(TourSection.entries.toSet(), initialSeenSections(isAuthorized = true))
    }

    @Test
    fun `a not-yet-authorized install starts with no sections seen`() {
        assertEquals(emptySet<TourSection>(), initialSeenSections(isAuthorized = false))
    }
}
