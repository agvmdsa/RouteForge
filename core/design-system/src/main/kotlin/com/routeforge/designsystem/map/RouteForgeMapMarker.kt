package com.routeforge.designsystem.map

/** How a marker is drawn: a plain colored dot, or a numbered badge (e.g. waypoint order). */
sealed interface RouteForgeMapMarkerIcon {
    data class Dot(
        val colorArgb: Int,
    ) : RouteForgeMapMarkerIcon

    data class Numbered(
        val number: Int,
        val backgroundColorArgb: Int,
        val textColorArgb: Int,
    ) : RouteForgeMapMarkerIcon
}

data class RouteForgeMapMarker(
    val id: Any,
    val latitude: Double,
    val longitude: Double,
    val icon: RouteForgeMapMarkerIcon,
    val draggable: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val onDragEnd: ((latitude: Double, longitude: Double) -> Unit)? = null,
)
