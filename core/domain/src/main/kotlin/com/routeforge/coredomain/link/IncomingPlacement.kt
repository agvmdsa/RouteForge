package com.routeforge.coredomain.link

import com.routeforge.coredomain.model.RoutePoint

/** What to do with something another app sent, once it is known which tab the user is on. */
sealed interface IncomingAction {
    /** On Plan Route: the add-waypoint confirmation. */
    data class AddWaypoint(
        val point: RoutePoint,
        val name: String?,
    ) : IncomingAction

    /** On any other tab: the Teleport / Save as favorite choice. */
    data class OfferChoice(
        val point: RoutePoint,
        val name: String?,
    ) : IncomingAction

    data class LoadRoute(
        val stops: List<RoutePoint>,
    ) : IncomingAction

    data class OpenSearch(
        val query: String,
        val onPlanRoute: Boolean,
    ) : IncomingAction

    /** An address that was opened, and held no position. */
    data object NoCoordinates : IncomingAction

    /** A shared text whose place could not be found. */
    data object NotFound : IncomingAction
}

object IncomingPlacement {
    fun actionFor(
        outcome: SharedTextOutcome,
        onPlanRoute: Boolean,
        wasAddress: Boolean,
    ): IncomingAction =
        when (outcome) {
            is SharedTextOutcome.Place ->
                if (onPlanRoute) {
                    IncomingAction.AddWaypoint(outcome.point, outcome.name)
                } else {
                    IncomingAction.OfferChoice(outcome.point, outcome.name)
                }
            is SharedTextOutcome.Route -> IncomingAction.LoadRoute(outcome.stops)
            is SharedTextOutcome.Search -> IncomingAction.OpenSearch(outcome.query, onPlanRoute)
            SharedTextOutcome.NotFound -> if (wasAddress) IncomingAction.NoCoordinates else IncomingAction.NotFound
        }
}
