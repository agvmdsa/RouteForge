package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteDraft

class AddWaypointUseCase {
    operator fun invoke(
        draft: RouteDraft,
        point: RoutePoint,
    ): RouteDraft = draft.add(point)
}
