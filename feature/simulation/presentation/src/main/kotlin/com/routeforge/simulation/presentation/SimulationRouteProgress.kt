package com.routeforge.simulation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.presentation.components.RouteProgressUi

/** Non-null as soon as a route is loaded, not only once playback starts — the card hosts the
 *  Cancel/Switch-mode/Waypoint-edit/Speed controls now (spec 011), which must be reachable the
 *  moment a route is loaded, same as the old fixed FABs they replaced. Before a mocked session
 *  exists yet (route loaded, not yet started), traveled distance is 0 and ETA is unknown — not an
 *  error, just nothing to report yet. Null whenever there's truly nothing to show progress for: no
 *  route loaded, a non-ROUTE session active (joystick/stationary have no "total distance" to
 *  measure against), or a route with no measurable length. Recomputed from [SimulationState] alone
 *  — no new domain calculation needed, the route's total distance and the session's traveled
 *  distance/speed (when a session exists) already carry everything this derives. */
@Composable
internal fun rememberRouteProgress(state: SimulationState): RouteProgressUi? {
    val session = state.mockedSession
    val route = state.loadedRoute
    if (route == null || route.distanceMeters <= 0.0 || (session != null && session.mode != SimulationMode.ROUTE)) {
        return null
    }
    return remember(session?.distanceTraveledMeters, route.distanceMeters, session?.speedMetersPerSecond) {
        val traveled = (session?.distanceTraveledMeters ?: 0.0).coerceIn(0.0, route.distanceMeters)
        RouteProgressUi(
            fractionComplete = (traveled / route.distanceMeters).toFloat(),
            traveledMeters = traveled,
            totalMeters = route.distanceMeters,
            etaSeconds =
                if (session != null && session.speedMetersPerSecond > 0f) {
                    (route.distanceMeters - traveled) / session.speedMetersPerSecond
                } else {
                    null
                },
        )
    }
}
