package com.routeforge.simulation.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.speed.SpeedSelectorDialog
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.presentation.R
import com.routeforge.simulation.presentation.SimulationAction
import com.routeforge.simulation.presentation.SimulationState
import com.routeforge.simulation.presentation.waypointedit.WaypointEditAction
import com.routeforge.simulation.presentation.waypointedit.WaypointEditState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ProgressCardShape = RoundedCornerShape(20.dp)
private val ProgressCardElevation = 4.dp
internal val ToggleIconButtonSize = 32.dp
internal val ControlIconButtonSize = 48.dp

/** How far past the card's resting position (bottom-center, set by the caller's own
 *  `Box.align`/padding) a drag is allowed to settle once released. Factors in the card's own
 *  measured width/height (via `onSizeChanged`), not just the screen's — a margin computed from
 *  screen size alone (the first version of this) left enough slack that the card could still end
 *  up partly or fully off-screen, since it never accounted for how wide the card itself is. */
private val HorizontalDragMargin = 24.dp
private val TopDragMargin = 48.dp
private val BottomDragSlack = 8.dp
private val SnapBackSpring = spring<Offset>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

/** [fractionComplete] is already clamped to `0f..1f`. [etaSeconds] is null when the current
 *  speed is zero — ETA is genuinely undefined then, not "arriving now". */
internal data class RouteProgressUi(
    val fractionComplete: Float,
    val traveledMeters: Double,
    val totalMeters: Double,
    val etaSeconds: Double?,
)

/** A collapsible, freely-draggable "how much of this route is left" card, in the same spirit as
 *  the ETA/distance panel navigation apps show mid-trip. Starts at [modifier]'s position (the
 *  caller's own alignment/padding); dragging it anywhere — in either expanded or collapsed state —
 *  just adds a running offset on top. Dragging is unclamped while the finger is down, but on
 *  release (or right after expanding/collapsing) it springs back within [HorizontalDragMargin]/
 *  [TopDragMargin]/[BottomDragSlack] of the screen edges — accounting for the card's own current
 *  size, so it never ends up partly or fully off-screen — instead of staying wherever it landed.
 *  The expand/collapse toggle lives on its own [IconButton] rather than a tap-anywhere-on-the-card
 *  gesture, specifically so it doesn't fight the drag gesture covering the rest of the card for
 *  the same touch. Neither the collapsed state nor the dragged position is persisted — both are
 *  transient viewing preferences, not settings, and reset the next time a route session starts. */
@Composable
internal fun RouteProgressCard(
    progress: RouteProgressUi,
    state: SimulationState,
    sessionDistanceTraveledMeters: Double?,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
    onAction: (SimulationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(true) }
    var isPlaybackSpeedDialogOpen by remember { mutableStateOf(false) }
    var cardSizePx by remember { mutableStateOf(IntSize.Zero) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val horizontalMarginPx = with(density) { HorizontalDragMargin.toPx() }
    val topMarginPx = with(density) { TopDragMargin.toPx() }
    val bottomSlackPx = with(density) { BottomDragSlack.toPx() }
    val restingBottomPx = with(density) { Spacing.ScreenContent.toPx() }

    // Returns [raw] unclamped if the card hasn't been measured yet (there's one frame between
    // first composing and the first onSizeChanged callback) — safer than clamping against a
    // not-yet-known size, which would read as "zero width/height" and clamp everything to zero.
    fun clampToScreen(raw: Offset): Offset {
        if (cardSizePx == IntSize.Zero) return raw
        val maxHorizontal = ((screenWidthPx - cardSizePx.width) / 2f - horizontalMarginPx).coerceAtLeast(0f)
        val maxUp = (screenHeightPx - cardSizePx.height - restingBottomPx - topMarginPx).coerceAtLeast(0f)
        return Offset(
            x = raw.x.coerceIn(-maxHorizontal, maxHorizontal),
            y = raw.y.coerceIn(-maxUp, bottomSlackPx),
        )
    }

    // Re-settles within bounds right after an expand/collapse toggle too — e.g. dragged near a
    // corner while collapsed, then expanded into a taller card that would otherwise hang off-screen.
    LaunchedEffect(isExpanded, cardSizePx) {
        val clamped = clampToScreen(offset.value)
        if (clamped != offset.value) offset.animateTo(clamped, SnapBackSpring)
    }

    val remainingMeters = (progress.totalMeters - progress.traveledMeters).coerceAtLeast(0.0)
    val percent = (progress.fractionComplete * 100).roundToInt()
    val etaLabel = progress.etaSeconds?.toEtaLabel() ?: stringResource(R.string.simulation_route_progress_eta_unknown)

    Surface(
        shape = ProgressCardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = ProgressCardElevation,
        modifier =
            modifier
                .onSizeChanged { cardSizePx = it }
                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            val clamped = clampToScreen(offset.value)
                            coroutineScope.launch { offset.animateTo(clamped, SnapBackSpring) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch { offset.snapTo(offset.value + dragAmount) }
                    }
                },
    ) {
        if (isExpanded) {
            RouteProgressCardExpanded(
                progress = progress,
                remainingMeters = remainingMeters,
                etaLabel = etaLabel,
                state = state,
                sessionDistanceTraveledMeters = sessionDistanceTraveledMeters,
                editState = editState,
                onEditAction = onEditAction,
                onAction = onAction,
                onCollapse = { isExpanded = false },
                onOpenPlaybackSpeedDialog = { isPlaybackSpeedDialogOpen = true },
            )
        } else {
            RouteProgressCardCollapsed(
                state = state,
                onAction = onAction,
                percent = percent,
                etaLabel = etaLabel,
                onExpand = { isExpanded = true },
            )
        }
    }

    if (isPlaybackSpeedDialogOpen) {
        PlaybackSpeedDialog(state = state, onAction = onAction, onDismiss = { isPlaybackSpeedDialogOpen = false })
    }
}

@Composable
private fun PlaybackSpeedDialog(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
    onDismiss: () -> Unit,
) {
    SpeedSelectorDialog(
        title = stringResource(R.string.simulation_playback_speed_dialog_title),
        speedLabel = stringResource(R.string.simulation_speed_value_label, state.playbackSpeedKmh.roundToInt()),
        confirmButtonLabel = stringResource(R.string.simulation_speed_dialog_close),
        speedKmh = state.playbackSpeedKmh,
        onSpeedChange = { kmh -> onAction(SimulationAction.OnPlaybackSpeedChange(kmh)) },
        onDismiss = onDismiss,
    )
}

private fun Double.toEtaLabel(): String {
    val totalMinutes = (this / 60.0).roundToInt().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}min" else "$minutes min"
}
