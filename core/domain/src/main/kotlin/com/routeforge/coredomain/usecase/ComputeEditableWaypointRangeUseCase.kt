package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.model.EditableWaypointRange
import com.routeforge.coredomain.model.Route

/** Spec 008 FR-012/FR-013: computes which of [Route.points] are still editable given how far a
 *  simulation has progressed. `null` [distanceTraveledMeters] (no active session) and an empty
 *  [Route.waypointCumulativeDistances] (boundary not yet known for this route) both resolve to
 *  "everything editable," never to "nothing editable." */
class ComputeEditableWaypointRangeUseCase {
    operator fun invoke(
        route: Route,
        distanceTraveledMeters: Double?,
    ): EditableWaypointRange {
        if (distanceTraveledMeters == null) return EditableWaypointRange(firstEditableIndex = 0)
        val cumulative = route.waypointCumulativeDistances
        if (cumulative.isEmpty()) return EditableWaypointRange(firstEditableIndex = 0)

        val firstEditableIndex = cumulative.indexOfFirst { it >= distanceTraveledMeters }
        return EditableWaypointRange(
            firstEditableIndex = if (firstEditableIndex == -1) route.points.size else firstEditableIndex,
        )
    }
}
