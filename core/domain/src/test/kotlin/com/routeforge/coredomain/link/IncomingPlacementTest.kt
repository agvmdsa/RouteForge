package com.routeforge.coredomain.link

import com.routeforge.coredomain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IncomingPlacementTest {
    private val point = RoutePoint(52.52, 13.405)
    private val stops = listOf(RoutePoint(52.51, 13.37), RoutePoint(52.52, 13.41))

    private fun action(
        outcome: SharedTextOutcome,
        onPlanRoute: Boolean = false,
        wasAddress: Boolean = false,
    ) = IncomingPlacement.actionFor(outcome, onPlanRoute, wasAddress)

    @Test
    fun `a place on Plan Route is added as a waypoint, with its name`() {
        assertEquals(
            IncomingAction.AddWaypoint(point, "Brandenburg Gate"),
            action(SharedTextOutcome.Place(point, "Brandenburg Gate"), onPlanRoute = true),
        )
    }

    @Test
    fun `a place on any other tab is offered as teleport or favorite, with its name`() {
        assertEquals(
            IncomingAction.OfferChoice(point, "Brandenburg Gate"),
            action(SharedTextOutcome.Place(point, "Brandenburg Gate"), onPlanRoute = false),
        )
    }

    @Test
    fun `a nameless place stays nameless on both`() {
        assertEquals(IncomingAction.AddWaypoint(point, null), action(SharedTextOutcome.Place(point, null), onPlanRoute = true))
        assertEquals(IncomingAction.OfferChoice(point, null), action(SharedTextOutcome.Place(point, null), onPlanRoute = false))
    }

    @Test
    fun `a route loads the same way on every tab`() {
        assertEquals(IncomingAction.LoadRoute(stops), action(SharedTextOutcome.Route(stops), onPlanRoute = true))
        assertEquals(IncomingAction.LoadRoute(stops), action(SharedTextOutcome.Route(stops), onPlanRoute = false))
    }

    @Test
    fun `a search opens the address search, remembering which tab it came from`() {
        val search = SharedTextOutcome.Search("Cafe Central")

        assertEquals(IncomingAction.OpenSearch("Cafe Central", onPlanRoute = true), action(search, onPlanRoute = true))
        assertEquals(IncomingAction.OpenSearch("Cafe Central", onPlanRoute = false), action(search, onPlanRoute = false))
    }

    @Test
    fun `nothing found for an opened address says it has no coordinates`() {
        assertEquals(IncomingAction.NoCoordinates, action(SharedTextOutcome.NotFound, wasAddress = true))
    }

    @Test
    fun `nothing found for a shared text says the place was not found`() {
        assertEquals(IncomingAction.NotFound, action(SharedTextOutcome.NotFound, wasAddress = false))
    }
}
