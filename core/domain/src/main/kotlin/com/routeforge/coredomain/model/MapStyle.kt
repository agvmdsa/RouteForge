package com.routeforge.coredomain.model

/** The map's visual tile style. Every option here resolves (see
 *  [com.routeforge.designsystem.map.RouteForgeMap]) to one of osmdroid's own bundled,
 *  globally-covering tile sources that are actually free for third-party apps to use — notably
 *  excluding osmdroid's own bundled Wikimedia source, whose tiles return HTTP 403 for any app
 *  that isn't Wikimedia or an affiliated site (confirmed by a real device hitting that error; its
 *  usage policy restricts it to the Wikimedia Movement). osmdroid ships no free satellite or
 *  dark-themed global tile source either, so neither is offered here; adding one would mean
 *  introducing a new third-party tile provider, a decision this app's OSM-only principle leaves
 *  to a deliberate choice, not a default. */
enum class MapStyle {
    /** The standard OpenStreetMap "Mapnik" style — what the map looked like before this setting existed. */
    STANDARD,

    /** Terrain/elevation-shaded style (OpenTopoMap). */
    TOPOGRAPHIC,
    ;

    companion object {
        val DEFAULT = STANDARD
    }
}
