package com.routeforge.coredomain

import com.routeforge.coredomain.model.NamedPlace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val METERS_PER_DEGREE_OF_LATITUDE = 111_194.9

class RecentPlacesTest {
    private val gate = NamedPlace(52.5163, 13.3777, "Brandenburg Gate")
    private val tower = NamedPlace(52.5208, 13.4094, "TV Tower")

    private fun metersNorthOf(
        place: NamedPlace,
        meters: Double,
        name: String? = place.name,
    ) = NamedPlace(place.latitude + meters / METERS_PER_DEGREE_OF_LATITUDE, place.longitude, name)

    private fun placesApart(count: Int) = List(count) { NamedPlace(10.0 + it * 0.01, 20.0, "Place $it") }

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

    @Test
    fun `using the same coordinates again leaves one entry, first`() {
        assertEquals(listOf(gate, tower), RecentPlaces.record(listOf(tower, gate), gate))
    }

    @Test
    fun `a place 5 meters away takes the entry's position with the new coordinates and name`() {
        val nearby = metersNorthOf(gate, 5.0, name = "Gate, east side")

        assertEquals(listOf(nearby), RecentPlaces.record(listOf(gate), nearby))
    }

    @Test
    fun `a place 9 meters away is the same place`() {
        assertEquals(1, RecentPlaces.record(listOf(gate), metersNorthOf(gate, 9.0)).size)
    }

    @Test
    fun `a place 22 meters away is a different place`() {
        assertEquals(2, RecentPlaces.record(listOf(gate), metersNorthOf(gate, 22.0)).size)
    }

    @Test
    fun `a nameless use of a named place keeps the entry's coordinates and name`() {
        val nameless = metersNorthOf(gate, 6.0, name = null)

        assertEquals(listOf(gate), RecentPlaces.record(listOf(gate), nameless))
    }

    @Test
    fun `a named use of a nameless entry takes the new coordinates and name`() {
        val nameless = NamedPlace(gate.latitude, gate.longitude, null)
        val named = metersNorthOf(gate, 6.0, name = "Gate")

        assertEquals(listOf(named), RecentPlaces.record(listOf(nameless), named))
    }

    @Test
    fun `two nameless uses of one place take the new coordinates and stay nameless`() {
        val first = NamedPlace(gate.latitude, gate.longitude, null)
        val second = metersNorthOf(gate, 6.0, name = null)

        assertEquals(listOf(second), RecentPlaces.record(listOf(first), second))
    }

    @Test
    fun `a new place in a full list goes first and the oldest entry drops off`() {
        val full = placesApart(RecentPlaces.MAX_PLACES)
        val fresh = NamedPlace(50.0, 50.0, "Fresh")

        val recorded = RecentPlaces.record(full, fresh)

        assertEquals(RecentPlaces.MAX_PLACES, recorded.size)
        assertEquals(listOf(fresh) + full.dropLast(1), recorded)
    }

    @Test
    fun `using the oldest entry of a full list again moves it to the top and keeps the others in order`() {
        val full = placesApart(RecentPlaces.MAX_PLACES)

        val recorded = RecentPlaces.record(full, full.last())

        assertEquals(listOf(full.last()) + full.dropLast(1), recorded)
    }

    @Test
    fun `a place near two entries that were already too close to each other replaces both`() {
        val first = NamedPlace(52.0, 13.0, "First")
        val second = metersNorthOf(first, 4.0, name = "Second")
        val between = metersNorthOf(first, 2.0, name = "Between")
        val elsewhere = NamedPlace(40.0, 10.0, "Elsewhere")

        val recorded = RecentPlaces.record(listOf(first, second, elsewhere), between)

        assertEquals(listOf(between, elsewhere), recorded)
    }
}
