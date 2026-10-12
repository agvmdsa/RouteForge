package com.routeforge.coredomain.model

/** The map's visual tile style. Every option here resolves (see
 *  [com.routeforge.designsystem.map.RouteForgeMap]) to one of osmdroid's own bundled,
 *  globally-covering tile sources — no API key, no new external service. osmdroid ships no free
 *  satellite or dark-themed global tile source, so neither is offered here; adding one would mean
 *  introducing a new third-party tile provider, a decision this app's OSM-only principle leaves
 *  to a deliberate choice, not a default. */
enum class MapStyle {
    /** The standard OpenStreetMap "Mapnik" style — what the map looked like before this setting existed. */
    STANDARD,

    /** A lighter, more muted cartographic style (Wikimedia's OSM-based tiles). */
    MINIMAL,

    /** Terrain/elevation-shaded style (OpenTopoMap). */
    TOPOGRAPHIC,
    ;

    companion object {
        val DEFAULT = STANDARD
    }
}
