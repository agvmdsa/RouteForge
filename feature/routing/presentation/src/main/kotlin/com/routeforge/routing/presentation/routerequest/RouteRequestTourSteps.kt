package com.routeforge.routing.presentation.routerequest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.tour.TourResultOutcome
import com.routeforge.coredomain.tour.TourStepKind
import com.routeforge.designsystem.components.TourHost
import com.routeforge.designsystem.components.TourStep
import com.routeforge.designsystem.components.TourStepResult
import com.routeforge.designsystem.components.TourTargets
import com.routeforge.designsystem.components.explanatoryTourStep
import com.routeforge.routing.presentation.R

/** Every Route Creation-tour step (spec 014), in order: placing the first two waypoints and pressing
 *  the mode switch are the action steps; the rest explain controls. [TourHost] skips a step whose
 *  control isn't on screen — e.g. Switch route mode only exists before a route has been computed.
 *  These are fuller explanations written for a first-time visitor, not the controls' short
 *  accessible names. */
@Composable
internal fun rememberRouteRequestTourSteps(state: RouteRequestState): List<TourStep> {
    // The no-path error is cleared by the next draft change, so "the attempt resolved without a
    // path" has to be latched to still count once it is gone. Lives as long as this screen's
    // composition, which is the tour's own scope — a revisit or a replay starts with it unset.
    var previewFailureSeen by remember { mutableStateOf(false) }
    LaunchedEffect(state.errorType) {
        if (state.errorType == RouteRequestError.MODE_PREVIEW_NO_PATH) previewFailureSeen = true
    }

    val pointCount = state.draft.points.size
    val hasEnoughPoints = pointCount >= MIN_WAYPOINTS_TO_PLAY
    val isGuidedPreview = state.previewMode == RoutePlaybackMode.GUIDED
    return listOf(
        TourStep(
            id = "routeReq_tap_map",
            text =
                stringResource(
                    if (pointCount == 0) R.string.routing_tour_desc_tap_map_two else R.string.routing_tour_desc_tap_map_one_more,
                ),
            kind = TourStepKind.ACTION,
            isSatisfied = hasEnoughPoints,
            isPaused = state.pendingAddLatitude != null,
        ),
        TourStep(
            id = "routeReq_switch_mode",
            text = stringResource(R.string.routing_tour_desc_switch_mode_action),
            kind = TourStepKind.ACTION,
            targetId = "routeReq_switch_mode",
            isApplicable = state.route == null && hasEnoughPoints,
            // Pressing the switch with no road data downloaded only opens the "missing regions"
            // sheet; the attempt counts once it resolves, with a Guided path or with no path at all.
            isSatisfied = isGuidedPreview || previewFailureSeen,
            isPaused = state.missingRegionsWarning != null,
            result =
                TourStepResult(
                    text = stringResource(R.string.routing_tour_desc_switch_mode_result),
                    // No callout for a change that never happened.
                    outcome = if (isGuidedPreview) TourResultOutcome.SHOW else TourResultOutcome.SKIP,
                ),
        ),
        explanatoryTourStep("routeReq_import", stringResource(R.string.routing_tour_desc_import)),
        explanatoryTourStep("routeReq_add_from_favorites", stringResource(R.string.routing_tour_desc_add_from_favorites)),
        explanatoryTourStep("routeReq_undo", stringResource(R.string.routing_tour_desc_undo)),
        explanatoryTourStep("routeReq_edit_waypoints", stringResource(R.string.routing_tour_desc_edit_waypoints)),
        explanatoryTourStep("routeReq_proceed", stringResource(R.string.routing_tour_desc_proceed)),
        explanatoryTourStep("routeReq_save_route", stringResource(R.string.routing_tour_desc_save_route)),
        // The Saved tab sits in the app's bottom bar, outside this screen — the tour finds it through
        // the shell's own registry.
        TourStep(id = "routeReq_saved_tab", text = stringResource(R.string.routing_tour_desc_saved_tab), targetId = TourTargets.SAVED_TAB),
    )
}
