package com.routeforge.coredomain

import com.routeforge.coredomain.model.NamedPlace

/** The rules of the recent places list: what a use of a place does to the list. */
object RecentPlaces {
    /** [current] is newest used first, and so is the result. */
    fun record(
        current: List<NamedPlace>,
        place: NamedPlace,
    ): List<NamedPlace> {
        if (!place.hasValidCoordinates()) return current
        return listOf(place.copy(name = place.name?.trim()?.takeIf { it.isNotEmpty() })) + current
    }

    private fun NamedPlace.hasValidCoordinates() = latitude in -90.0..90.0 && longitude in -180.0..180.0
}
