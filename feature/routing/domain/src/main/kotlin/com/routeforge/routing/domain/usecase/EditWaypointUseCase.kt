package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteDraft

class EditWaypointUseCase {
    operator fun invoke(
        draft: RouteDraft,
        index: Int,
        newValue: RoutePoint,
    ): RouteDraft = draft.update(index, newValue)
}
