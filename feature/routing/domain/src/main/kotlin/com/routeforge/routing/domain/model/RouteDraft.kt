package com.routeforge.routing.domain.model

import com.routeforge.coredomain.model.RoutePoint

private const val MAX_HISTORY = 50

/**
 * In-progress, immutable waypoint list for manual route building, with a bounded undo history.
 * Equality between waypoints is plain [RoutePoint] `equals()` (bit-for-bit `Double` comparison) —
 * near-identical points are never merged or deduplicated (FR-009a).
 */
data class RouteDraft(
    val points: List<RoutePoint> = emptyList(),
    private val history: List<List<RoutePoint>> = emptyList(),
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    fun add(point: RoutePoint): RouteDraft = withNewPoints(points + point)

    fun update(
        index: Int,
        newValue: RoutePoint,
    ): RouteDraft = withNewPoints(points.toMutableList().apply { this[index] = newValue })

    fun delete(index: Int): RouteDraft = withNewPoints(points.toMutableList().apply { removeAt(index) })

    fun undo(): RouteDraft {
        val previous = history.lastOrNull() ?: return this
        return RouteDraft(points = previous, history = history.dropLast(1))
    }

    private fun withNewPoints(newPoints: List<RoutePoint>): RouteDraft =
        RouteDraft(points = newPoints, history = (history + listOf(points)).takeLast(MAX_HISTORY))
}
