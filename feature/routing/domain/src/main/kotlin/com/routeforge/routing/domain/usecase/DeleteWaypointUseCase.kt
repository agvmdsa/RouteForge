package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.model.RouteDraft

class DeleteWaypointUseCase {
    operator fun invoke(
        draft: RouteDraft,
        index: Int,
    ): RouteDraft = draft.delete(index)
}
