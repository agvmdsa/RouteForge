package com.routeforge.coredomain.model

/** A WGS84 coordinate pair — gives a name to what used to flow through route geometry, camera
 *  targets, and map overlays as a bare `Pair<Double, Double>` (latitude first, by a convention
 *  the type itself never enforced). Not a `@JvmInline value class`: Kotlin's inline value classes
 *  can only wrap a single underlying property, and two `Double`s can't be packed into one without
 *  losing precision, so this is a plain data class instead — still a value object in the sense
 *  that matters here (named, immutable, structurally compared). */
data class LatLng(
    val latitude: Double,
    val longitude: Double,
)
