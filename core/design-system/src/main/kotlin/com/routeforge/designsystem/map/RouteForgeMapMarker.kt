package com.routeforge.designsystem.map

/** How a marker is drawn: a plain colored dot, a numbered badge (e.g. waypoint order), or a
 *  directional arrow (e.g. the current mocked position, rotated to face its bearing). */
sealed interface RouteForgeMapMarkerIcon {
    data class Dot(
        val colorArgb: Int,
    ) : RouteForgeMapMarkerIcon

    data class Numbered(
        val number: Int,
        val backgroundColorArgb: Int,
        val textColorArgb: Int,
    ) : RouteForgeMapMarkerIcon

    data class Arrow(
        val colorArgb: Int,
    ) : RouteForgeMapMarkerIcon
}

data class RouteForgeMapMarker(
    val id: Any,
    val latitude: Double,
    val longitude: Double,
    val icon: RouteForgeMapMarkerIcon,
    val draggable: Boolean = false,
    /** Clockwise rotation from north, in degrees — only meaningful for [RouteForgeMapMarkerIcon.Arrow]. */
    val rotationDegrees: Float = 0f,
    val onClick: (() -> Unit)? = null,
    val onDragEnd: ((latitude: Double, longitude: Double) -> Unit)? = null,
)
