package com.routeforge.simulation.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.routeforge.designsystem.components.AutoDismissConfirmationPill
import com.routeforge.designsystem.components.FabColumn
import com.routeforge.designsystem.components.TooltipAnchor
import com.routeforge.designsystem.components.TopBanner
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.designsystem.speed.SpeedSelectorDialog
import com.routeforge.designsystem.speed.SpeedSelectorFab
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.presentation.components.CoordinatePill
import com.routeforge.simulation.presentation.components.Joystick
import kotlin.math.roundToInt

private const val MIN_SPEED_KMH = 0f
private const val MAX_SPEED_KMH = 150f
private val SpeedSelectorRangeKmh = MIN_SPEED_KMH..MAX_SPEED_KMH

/** Top-anchored overlays: the coordinate pill, the authorization/error banner, and the transient
 *  "waypoints updated" confirmation. */
@Composable
internal fun BoxScope.SimulationTopOverlays(
    state: SimulationState,
    showWaypointsUpdatedConfirmation: Boolean,
    onDismissWaypointsUpdatedConfirmation: () -> Unit,
    onAction: (SimulationAction) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = Spacing.ScreenContent, start = Spacing.ScreenContent, end = Spacing.ScreenContent),
        contentAlignment = Alignment.Center,
    ) {
        CoordinatePill(
            mockedSession = state.mockedSession,
            isSearchingRealLocation = state.isSearchingRealLocation,
            hasKnownRealLocation = state.realLocation != null,
            onCancelMockClick = { onAction(SimulationAction.OnCancelMockClick) },
        )
    }

    if (state.isBlockedByAuthorization) {
        TopBanner(
            contentPadding = Spacing.ScreenContent,
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter,
                    ).padding(top = Spacing.BannerTopOffset, start = Spacing.ScreenContent, end = Spacing.ScreenContent),
        ) {
            Text(stringResource(R.string.simulation_error_not_authorized))
            Button(onClick = { onAction(SimulationAction.OnOpenSetupClick) }) {
                Text(stringResource(R.string.simulation_go_to_setup_button))
            }
        }
    } else {
        state.errorType?.let { errorType ->
            TopBanner(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter,
                        ).padding(top = Spacing.BannerTopOffset, start = Spacing.ScreenContent, end = Spacing.ScreenContent),
            ) {
                Text(text = errorType.toMessage())
            }
        }
    }

    AutoDismissConfirmationPill(
        message = stringResource(R.string.simulation_waypoints_updated_message),
        isVisible = showWaypointsUpdatedConfirmation,
        onDismiss = onDismissWaypointsUpdatedConfirmation,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.BannerTopOffset),
    )
}

/** Bottom-start FAB column (joystick speed selector, joystick toggle) plus the joystick itself and
 *  its speed dialog — grouped together since the dialog's open/closed state is only ever toggled
 *  from this column's own FAB. Route playback no longer has controls here (spec 011) — the
 *  joystick and an active route remain mutually exclusive, so this column is only ever shown when
 *  no route is loaded. */
@Composable
internal fun BoxScope.SimulationMovementControls(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    if (state.loadedRoute != null) return
    var isJoystickSpeedDialogOpen by remember { mutableStateOf(false) }
    FabColumn(alignment = Alignment.BottomStart) {
        if (state.isJoystickVisible) {
            val joystickSpeedDescription = stringResource(R.string.simulation_joystick_speed_button)
            TooltipAnchor(tooltipText = joystickSpeedDescription) {
                SpeedSelectorFab(
                    onClick = { isJoystickSpeedDialogOpen = true },
                    contentDescription = joystickSpeedDescription,
                    modifier = Modifier.tourTarget("sim_joystick_speed"),
                )
            }
        }
        val joystickToggleDescription = stringResource(R.string.simulation_joystick_label)
        TooltipAnchor(tooltipText = joystickToggleDescription) {
            FloatingActionButton(
                onClick = { onAction(SimulationAction.OnToggleJoystick) },
                modifier = Modifier.tourTarget("sim_joystick_toggle"),
            ) {
                Icon(Icons.Filled.SportsEsports, contentDescription = joystickToggleDescription)
            }
        }
    }

    if (state.isJoystickVisible) {
        Joystick(
            onDrag = { bearing -> onAction(SimulationAction.OnJoystickDrag(bearing)) },
            onReleased = { onAction(SimulationAction.OnJoystickReleased) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.ScreenContent),
        )
        if (isJoystickSpeedDialogOpen) {
            SpeedSelectorDialog(
                title = stringResource(R.string.simulation_joystick_speed_dialog_title),
                speedLabel = stringResource(R.string.simulation_speed_value_label, state.joystickSpeedKmh.roundToInt()),
                confirmButtonLabel = stringResource(R.string.simulation_speed_dialog_close),
                speedKmh = state.joystickSpeedKmh,
                onSpeedChange = { kmh -> onAction(SimulationAction.OnJoystickSpeedChange(kmh)) },
                onDismiss = { isJoystickSpeedDialogOpen = false },
                speedRangeKmh = SpeedSelectorRangeKmh,
            )
        }
    }
}

@Composable
private fun SimulationErrorType.toMessage(): String =
    when (this) {
        SimulationErrorType.NOT_AUTHORIZED -> stringResource(R.string.simulation_error_not_authorized)
        SimulationErrorType.INVALID_SPEED -> stringResource(R.string.simulation_error_invalid_speed)
        SimulationErrorType.INVALID_EXECUTION_TIMES -> stringResource(R.string.simulation_error_invalid_execution_times)
        SimulationErrorType.JOYSTICK_NO_REAL_FIX -> stringResource(R.string.simulation_joystick_no_fix_message)
        SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED -> stringResource(R.string.simulation_error_real_location_provider_disabled)
        SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED -> stringResource(R.string.simulation_error_real_location_permission_denied)
        SimulationErrorType.REAL_LOCATION_TIMED_OUT -> stringResource(R.string.simulation_error_real_location_timed_out)
    }

@Preview
@Composable
private fun SimulationTopOverlaysPreview() {
    RouteForgeTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SimulationTopOverlays(
                state =
                    SimulationState(
                        isBlockedByAuthorization = true,
                        errorType = SimulationErrorType.NOT_AUTHORIZED,
                    ),
                showWaypointsUpdatedConfirmation = false,
                onDismissWaypointsUpdatedConfirmation = {},
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun SimulationMovementControlsPreview() {
    RouteForgeTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SimulationMovementControls(state = SimulationState(isJoystickVisible = true), onAction = {})
        }
    }
}
