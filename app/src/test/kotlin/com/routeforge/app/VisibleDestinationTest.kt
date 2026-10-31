package com.routeforge.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VisibleDestinationTest {
    private val isSearch: (String) -> Boolean = { it == "search" }

    @Test
    fun `with the search on top the screen under it is the visible one`() {
        assertEquals("simulate", visibleDestination(current = "search", previous = "simulate", isSearch = isSearch))
    }

    @Test
    fun `with a tab root on top it is the visible one`() {
        assertEquals("plan", visibleDestination(current = "plan", previous = "simulate", isSearch = isSearch))
    }

    @Test
    fun `with no destination on top nothing is visible`() {
        assertNull(visibleDestination(current = null, previous = "simulate", isSearch = isSearch))
    }

    @Test
    fun `with the search on top and nothing under it nothing is visible`() {
        assertNull(visibleDestination(current = "search", previous = null, isSearch = isSearch))
    }
}
