package com.routeforge.coredomain

import com.routeforge.coredomain.model.NamedPlace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecentPlacesTest {
    private val gate = NamedPlace(52.5163, 13.3777, "Brandenburg Gate")
    private val tower = NamedPlace(52.5208, 13.4094, "TV Tower")

    @Test
    fun `recording a place in an empty list gives a list of that place`() {
        assertEquals(listOf(gate), RecentPlaces.record(emptyList(), gate))
    }

    @Test
    fun `a latitude outside minus 90 to 90 leaves the list unchanged`() {
        val current = listOf(gate)

        assertEquals(current, RecentPlaces.record(current, NamedPlace(91.0, 13.0)))
        assertEquals(current, RecentPlaces.record(current, NamedPlace(-91.0, 13.0)))
    }

    @Test
    fun `a longitude outside minus 180 to 180 leaves the list unchanged`() {
        val current = listOf(gate)

        assertEquals(current, RecentPlaces.record(current, NamedPlace(52.0, 181.0)))
        assertEquals(current, RecentPlaces.record(current, NamedPlace(52.0, -181.0)))
    }

    @Test
    fun `a coordinate that is not a number leaves the list unchanged`() {
        val current = listOf(gate)

        assertEquals(current, RecentPlaces.record(current, NamedPlace(Double.NaN, 13.0)))
        assertEquals(current, RecentPlaces.record(current, NamedPlace(52.0, Double.NaN)))
    }

    @Test
    fun `a name of only spaces is stored as no name`() {
        val recorded = RecentPlaces.record(emptyList(), NamedPlace(52.0, 13.0, "   "))

        assertEquals(listOf(NamedPlace(52.0, 13.0, null)), recorded)
    }

    @Test
    fun `a name with spaces around it is stored trimmed`() {
        val recorded = RecentPlaces.record(emptyList(), NamedPlace(52.0, 13.0, "  Home  "))

        assertEquals(listOf(NamedPlace(52.0, 13.0, "Home")), recorded)
    }

    @Test
    fun `a second different place goes first and the earlier one second`() {
        assertEquals(listOf(tower, gate), RecentPlaces.record(listOf(gate), tower))
    }
}
