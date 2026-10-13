package com.routeforge.simulation.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.presentation.R
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val ProgressCardShape = RoundedCornerShape(20.dp)
private val ProgressCardElevation = 4.dp
private val ProgressCardPadding = 16.dp
private val ProgressCardCollapsedHorizontalPadding = 16.dp
private val ProgressCardCollapsedVerticalPadding = 8.dp
private val ProgressCardRowSpacing = 8.dp
private val ToggleIconButtonSize = 32.dp
private const val METERS_PER_KILOMETER = 1_000.0
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
 *  the ETA/distance panel navigation apps show mid-trip. Positions itself absolutely within
 *  [containerSizePx] (bottom-center on first layout) instead of relying on the caller's
 *  `Box.align`, since dragging needs to reason about the card's position in the same pixel space
 *  it clamps against. Dragging is unclamped while the finger is down — it can be thrown past the
 *  edge of the screen — but on release (or whenever expanding/collapsing would otherwise leave it
 *  out of bounds) it springs back into bounds with a bouncy animation rather than an abrupt snap.
 *  The expand/collapse toggle lives on its own [IconButton] rather than a tap-anywhere-on-the-card
 *  gesture, specifically so it doesn't fight the drag gesture covering the rest of the card for
 *  the same touch. Neither the collapsed state nor the dragged position is persisted — both are
 *  transient viewing preferences, not settings, and reset the next time a route session starts. */
@Composable
internal fun RouteProgressCard(
    progress: RouteProgressUi,
    containerSizePx: IntSize,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(true) }
    var cardSizePx by remember { mutableStateOf(IntSize.Zero) }
    var hasPositioned by remember { mutableStateOf(false) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()
    val marginPx = with(LocalDensity.current) { Spacing.ScreenContent.toPx() }

    // Runs once the card's first real size is known (places it bottom-center), and again every
    // time cardSizePx changes afterward (an expand/collapse toggle) — if the now-differently-sized
    // card no longer fits where it was, this is what springs it back into bounds.
    LaunchedEffect(cardSizePx, containerSizePx) {
        if (cardSizePx == IntSize.Zero || containerSizePx == IntSize.Zero) return@LaunchedEffect
        if (!hasPositioned) {
            val initialX = ((containerSizePx.width - cardSizePx.width) / 2f).coerceAtLeast(marginPx)
            val initialY = (containerSizePx.height - cardSizePx.height - marginPx).coerceAtLeast(marginPx)
            offset.snapTo(Offset(initialX, initialY))
            hasPositioned = true
        } else {
            val clamped = offset.value.clampToBounds(cardSizePx, containerSizePx, marginPx)
            if (clamped != offset.value) offset.animateTo(clamped, SnapBackSpring)
        }
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
                .animateContentSize()
                .onSizeChanged { cardSizePx = it }
                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                .pointerInput(containerSizePx) {
                    detectDragGestures(
                        onDragEnd = {
                            val clamped = offset.value.clampToBounds(cardSizePx, containerSizePx, marginPx)
                            coroutineScope.launch { offset.animateTo(clamped, SnapBackSpring) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch { offset.snapTo(offset.value + dragAmount) }
                    }
                },
    ) {
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(ProgressCardPadding),
                verticalArrangement = Arrangement.spacedBy(ProgressCardRowSpacing),
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
                    IconButton(onClick = { isExpanded = false }, modifier = Modifier.size(ToggleIconButtonSize)) {
                        Icon(
                            Icons.Filled.ExpandLess,
                            contentDescription = stringResource(R.string.simulation_route_progress_collapse),
                        )
                    }
                }
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
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.ControlsRow),
                modifier =
                    Modifier.padding(
                        start = ProgressCardCollapsedHorizontalPadding,
                        end = Spacing.ControlsRow,
                        top = ProgressCardCollapsedVerticalPadding,
                        bottom = ProgressCardCollapsedVerticalPadding,
                    ),
            ) {
                Text(
                    text = stringResource(R.string.simulation_route_progress_collapsed_label, percent, etaLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { isExpanded = true }, modifier = Modifier.size(ToggleIconButtonSize)) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.simulation_route_progress_expand),
                    )
                }
            }
        }
    }
}

private fun Double.toKilometersLabel(): String = "%.1f km".format(Locale.US, this / METERS_PER_KILOMETER)

private fun Double.toEtaLabel(): String {
    val totalMinutes = (this / 60.0).roundToInt().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}min" else "$minutes min"
}

/** Keeps the card's top-left corner at least [marginPx] away from every edge of
 *  [containerSizePx] given the card's own current [cardSizePx] — the same margin [RouteProgressCard]
 *  places it at initially, so a drag that ends up back near an edge settles at the same spot it
 *  started from. Falls back to pinning against the near edge (rather than centering or producing a
 *  negative range) on the rare container that's smaller than the card plus both margins. */
private fun Offset.clampToBounds(
    cardSizePx: IntSize,
    containerSizePx: IntSize,
    marginPx: Float,
): Offset {
    val maxX = (containerSizePx.width - cardSizePx.width - marginPx).coerceAtLeast(marginPx)
    val maxY = (containerSizePx.height - cardSizePx.height - marginPx).coerceAtLeast(marginPx)
    return Offset(x.coerceIn(marginPx, maxX), y.coerceIn(marginPx, maxY))
}
