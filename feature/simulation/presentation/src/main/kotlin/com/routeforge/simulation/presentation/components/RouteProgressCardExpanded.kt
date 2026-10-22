package com.routeforge.simulation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.designsystem.components.TooltipAnchor
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.presentation.R
import com.routeforge.simulation.presentation.SimulationAction
import com.routeforge.simulation.presentation.SimulationState
import com.routeforge.simulation.presentation.WaypointEditTriggerButton
import com.routeforge.simulation.presentation.waypointedit.WaypointEditAction
import com.routeforge.simulation.presentation.waypointedit.WaypointEditState
import java.util.Locale

private val ProgressCardPadding = 16.dp
private val ProgressCardRowSpacing = 8.dp
private const val METERS_PER_KILOMETER = 1_000.0

@Composable
internal fun RouteProgressCardExpanded(
    progress: RouteProgressUi,
    remainingMeters: Double,
    etaLabel: String,
    state: SimulationState,
    sessionDistanceTraveledMeters: Double?,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
    onAction: (SimulationAction) -> Unit,
    onCollapse: () -> Unit,
    onOpenPlaybackSpeedDialog: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(ProgressCardPadding),
        verticalArrangement = Arrangement.spacedBy(ProgressCardRowSpacing),
    ) {
        RouteProgressCardHeader(etaLabel = etaLabel, onCollapse = onCollapse)
        RouteProgressCardFigures(progress = progress, remainingMeters = remainingMeters)
        RouteProgressCardControls(
            state = state,
            sessionDistanceTraveledMeters = sessionDistanceTraveledMeters,
            editState = editState,
            onEditAction = onEditAction,
            onAction = onAction,
            onOpenPlaybackSpeedDialog = onOpenPlaybackSpeedDialog,
        )
    }
}

@Composable
private fun RouteProgressCardHeader(
    etaLabel: String,
    onCollapse: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ControlsRow),
    ) {
        Icon(Icons.Filled.DirectionsCar, contentDescription = null)
        Text(
            text = stringResource(R.string.simulation_route_progress_eta, etaLabel),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        val collapseDescription = stringResource(R.string.simulation_route_progress_collapse)
        TooltipAnchor(tooltipText = collapseDescription) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(ToggleIconButtonSize).tourTarget("sim_collapse_toggle"),
            ) {
                Icon(Icons.Filled.ExpandLess, contentDescription = collapseDescription)
            }
        }
    }
}

@Composable
private fun RouteProgressCardFigures(
    progress: RouteProgressUi,
    remainingMeters: Double,
) {
    LinearProgressIndicator(
        progress = { progress.fractionComplete },
        modifier = Modifier.fillMaxWidth(),
        strokeCap = StrokeCap.Round,
    )
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.simulation_route_progress_remaining, remainingMeters.toKilometersLabel()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                stringResource(
                    R.string.simulation_route_progress_traveled_of_total,
                    progress.traveledMeters.toKilometersLabel(),
                    progress.totalMeters.toKilometersLabel(),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RouteProgressCardControls(
    state: SimulationState,
    sessionDistanceTraveledMeters: Double?,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
    onAction: (SimulationAction) -> Unit,
    onOpenPlaybackSpeedDialog: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ControlsRow),
    ) {
        val cancelDescription = stringResource(R.string.simulation_cancel_route_button)
        TooltipAnchor(tooltipText = cancelDescription) {
            IconButton(
                onClick = { onAction(SimulationAction.OnCancelRouteClick) },
                modifier = Modifier.size(ControlIconButtonSize).tourTarget("sim_cancel_route"),
            ) {
                Icon(Icons.Filled.Close, contentDescription = cancelDescription)
            }
        }
        PlaybackButton(
            state = state,
            onAction = onAction,
            modifier = Modifier.size(ControlIconButtonSize).tourTarget("sim_playback"),
        )
        val activeRoute = state.loadedRoute
        if (activeRoute?.alternateGeometry != null) {
            val switchModeDescription = stringResource(R.string.simulation_switch_route_mode_button)
            val switchModeTooltip = stringResource(R.string.simulation_switch_route_mode_tooltip)
            TooltipAnchor(tooltipText = switchModeTooltip) {
                IconButton(
                    onClick = { onAction(SimulationAction.OnSwitchRouteModeClick) },
                    modifier = Modifier.size(ControlIconButtonSize).tourTarget("sim_switch_mode"),
                ) {
                    Icon(
                        imageVector =
                            if (activeRoute.mode == RoutePlaybackMode.GUIDED) {
                                Icons.Filled.Signpost
                            } else {
                                Icons.Filled.Straighten
                            },
                        contentDescription = switchModeDescription,
                    )
                }
            }
        }
        WaypointEditTriggerButton(
            loadedRoute = state.loadedRoute,
            distanceTraveledMeters = sessionDistanceTraveledMeters,
            editState = editState,
            onEditAction = onEditAction,
            modifier = Modifier.size(ControlIconButtonSize).tourTarget("sim_edit_route"),
        )
        val speedDescription = stringResource(R.string.simulation_playback_speed_button)
        TooltipAnchor(tooltipText = speedDescription) {
            IconButton(
                onClick = onOpenPlaybackSpeedDialog,
                modifier = Modifier.size(ControlIconButtonSize).tourTarget("sim_playback_speed_selector"),
            ) {
                Icon(Icons.Filled.Speed, contentDescription = speedDescription)
            }
        }
    }
}

private fun Double.toKilometersLabel(): String = "%.1f km".format(Locale.US, this / METERS_PER_KILOMETER)

private val PreviewProgress =
    RouteProgressUi(
        fractionComplete = 0.4f,
        traveledMeters = 4_000.0,
        totalMeters = 10_000.0,
        etaSeconds = 600.0,
    )

@Preview
@Composable
private fun RouteProgressCardExpandedPreview() {
    RouteForgeTheme {
        Surface {
            RouteProgressCardExpanded(
                progress = PreviewProgress,
                remainingMeters = 6_000.0,
                etaLabel = "10 min",
                state = SimulationState(),
                sessionDistanceTraveledMeters = 4_000.0,
                editState = WaypointEditState(),
                onEditAction = {},
                onAction = {},
                onCollapse = {},
                onOpenPlaybackSpeedDialog = {},
            )
        }
    }
}

@Preview
@Composable
private fun RouteProgressCardHeaderPreview() {
    RouteForgeTheme {
        Surface { RouteProgressCardHeader(etaLabel = "10 min", onCollapse = {}) }
    }
}

@Preview
@Composable
private fun RouteProgressCardFiguresPreview() {
    RouteForgeTheme {
        Surface {
            Column {
                RouteProgressCardFigures(
                    progress = PreviewProgress,
                    remainingMeters = 6_000.0,
                )
            }
        }
    }
}

@Preview
@Composable
private fun RouteProgressCardControlsPreview() {
    RouteForgeTheme {
        Surface {
            RouteProgressCardControls(
                state = SimulationState(),
                sessionDistanceTraveledMeters = null,
                editState = WaypointEditState(),
                onEditAction = {},
                onAction = {},
                onOpenPlaybackSpeedDialog = {},
            )
        }
    }
}
