package com.routeforge.simulation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.routeforge.coredomain.tour.TourStepKind
import com.routeforge.designsystem.components.TourHost
import com.routeforge.designsystem.components.TourStep
import com.routeforge.designsystem.components.TourTargets
import com.routeforge.designsystem.components.explanatoryTourStep
import com.routeforge.simulation.domain.model.SimulationMode

/** Every Simulation-tour step (spec 014), in order. Tapping the map to start a simulation is the one
 *  action step; the rest explain controls. Unlike [specs/012-simulation-screen-tooltips]'s long-press
 *  tooltips — which must match each control's accessible name exactly — these are their own fuller
 *  explanations, written for someone seeing the control for the first time (e.g. calling out that the
 *  joystick speed button only appears once the joystick itself is active). Not every step is on
 *  screen at once (e.g. route controls vs. joystick controls are mutually exclusive, and the
 *  Expand/Collapse toggle is only ever one or the other) — [TourHost] skips a step whose control
 *  isn't there, and the state rules below skip the ones that make no sense for the current state. */
@Composable
internal fun rememberSimulationTourSteps(state: SimulationState): List<TourStep> {
    val mockedSession = state.mockedSession
    return listOf(
        TourStep(
            id = "sim_tap_map",
            text = stringResource(R.string.simulation_tour_desc_tap_map),
            kind = TourStepKind.ACTION,
            // Tapping the map only starts a simulation while no route is loaded.
            isApplicable = state.loadedRoute == null,
            isSatisfied = mockedSession != null,
            isPaused = state.pendingTeleportLatitude != null,
        ),
        explanatoryTourStep(
            "sim_cancel_mock",
            stringResource(R.string.simulation_tour_desc_cancel_mock),
            isApplicable = mockedSession?.mode == SimulationMode.STATIONARY,
        ),
        explanatoryTourStep(
            "sim_coordinate_text",
            stringResource(R.string.simulation_tour_desc_coordinate_text),
            isApplicable = mockedSession != null,
        ),
        explanatoryTourStep("sim_cancel_route", stringResource(R.string.simulation_tour_desc_cancel_route)),
        explanatoryTourStep("sim_playback", stringResource(R.string.simulation_tour_desc_playback)),
        explanatoryTourStep("sim_switch_mode", stringResource(R.string.simulation_tour_desc_switch_mode)),
        explanatoryTourStep("sim_edit_route", stringResource(R.string.simulation_tour_desc_edit_route)),
        explanatoryTourStep("sim_playback_speed_selector", stringResource(R.string.simulation_tour_desc_playback_speed)),
        explanatoryTourStep("sim_collapse_toggle", stringResource(R.string.simulation_tour_desc_collapse)),
        explanatoryTourStep("sim_expand_toggle", stringResource(R.string.simulation_tour_desc_expand)),
        explanatoryTourStep("sim_joystick_toggle", stringResource(R.string.simulation_tour_desc_joystick_toggle)),
        explanatoryTourStep("sim_joystick_speed", stringResource(R.string.simulation_tour_desc_joystick_speed)),
        // The Saved tab sits in the app's bottom bar, outside this screen — the tour finds it through
        // the shell's own registry.
        TourStep(id = "sim_saved_tab", text = stringResource(R.string.simulation_tour_desc_saved_tab), targetId = TourTargets.SAVED_TAB),
    )
}
