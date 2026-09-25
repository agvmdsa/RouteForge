package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteDraft

class MoveWaypointUseCase {
    operator fun invoke(
        draft: RouteDraft,
        index: Int,
        to: RoutePoint,
    ): RouteDraft = draft.update(index, to)
}
