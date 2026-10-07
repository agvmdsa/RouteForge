package com.routeforge.designsystem.map

/** A rectangular area to paint on [RouteForgeMap], outlined in black and filled only when
 *  [filled] is true — used to show which map regions already have offline data on the device. */
data class RouteForgeMapRegionOverlay(
    val id: Any,
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
    val filled: Boolean,
)
