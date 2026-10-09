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
    /** Fires continuously while the marker is being dragged (not just on release) — lets a caller
     *  live-update a polyline preview as the user drags. A faded "ghost" marker is drawn at the
     *  pre-drag position automatically for the duration of the gesture, with no caller involvement
     *  needed. */
    val onDrag: ((latitude: Double, longitude: Double) -> Unit)? = null,
    val onDragEnd: ((latitude: Double, longitude: Double) -> Unit)? = null,
)
