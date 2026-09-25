package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.model.RouteDraft

class UndoRouteDraftUseCase {
    operator fun invoke(draft: RouteDraft): RouteDraft = draft.undo()
}
