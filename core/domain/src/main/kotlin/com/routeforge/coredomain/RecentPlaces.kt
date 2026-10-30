package com.routeforge.coredomain

import com.routeforge.coredomain.model.NamedPlace

/** The rules of the recent places list: what a use of a place does to the list. */
object RecentPlaces {
    const val MAX_PLACES = 20
    const val SAME_PLACE_METERS = 10.0

    /**
     * [current] is newest used first, and so is the result. Entries within [SAME_PLACE_METERS] of the place are
     * the same place and are replaced by one entry at the top. That entry takes the new use's coordinates and
     * name, except that a nameless use keeps the coordinates and name of the newest named entry it replaces.
     */
    fun record(
        current: List<NamedPlace>,
        place: NamedPlace,
    ): List<NamedPlace> {
        if (!place.hasValidCoordinates()) return current
        val used = place.copy(name = place.name?.trim()?.takeIf { it.isNotEmpty() })
        val (samePlace, others) = current.partition { it.isSamePlaceAs(used) }
        val entry = if (used.name == null) samePlace.firstOrNull { it.name != null } ?: used else used
        return (listOf(entry) + others).take(MAX_PLACES)
    }

    private fun NamedPlace.hasValidCoordinates() = latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun NamedPlace.isSamePlaceAs(other: NamedPlace) =
        GeoMath.haversineMeters(latitude, longitude, other.latitude, other.longitude) <= SAME_PLACE_METERS
}
