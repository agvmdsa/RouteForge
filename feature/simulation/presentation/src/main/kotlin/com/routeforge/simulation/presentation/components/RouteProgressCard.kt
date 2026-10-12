package com.routeforge.simulation.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.presentation.R
import java.util.Locale
import kotlin.math.roundToInt

private val ProgressCardShape = RoundedCornerShape(20.dp)
private val ProgressCardElevation = 4.dp
private val ProgressCardPadding = 16.dp
private val ProgressCardCollapsedHorizontalPadding = 16.dp
private val ProgressCardCollapsedVerticalPadding = 8.dp
private val ProgressCardRowSpacing = 8.dp
private const val METERS_PER_KILOMETER = 1_000.0

/** [fractionComplete] is already clamped to `0f..1f`. [etaSeconds] is null when the current
 *  speed is zero — ETA is genuinely undefined then, not "arriving now". */
internal data class RouteProgressUi(
    val fractionComplete: Float,
    val traveledMeters: Double,
    val totalMeters: Double,
    val etaSeconds: Double?,
)

/** A collapsible "how much of this route is left" card, in the same spirit as the ETA/distance
 *  panel navigation apps show mid-trip. Starts expanded; tapping anywhere on it toggles between
 *  the full breakdown and a single compact line — remembered per-composition (not persisted),
 *  since it's a transient viewing preference, not a setting. */
@Composable
internal fun RouteProgressCard(
    progress: RouteProgressUi,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(true) }
    val remainingMeters = (progress.totalMeters - progress.traveledMeters).coerceAtLeast(0.0)
    val percent = (progress.fractionComplete * 100).roundToInt()
    val etaLabel = progress.etaSeconds?.toEtaLabel() ?: stringResource(R.string.simulation_route_progress_eta_unknown)
    val collapseLabel = stringResource(R.string.simulation_route_progress_collapse)
    val expandLabel = stringResource(R.string.simulation_route_progress_expand)

    Surface(
        shape = ProgressCardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = ProgressCardElevation,
        modifier =
            modifier.clickable(onClickLabel = if (isExpanded) collapseLabel else expandLabel) {
                isExpanded = !isExpanded
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
                    Icon(Icons.Filled.ExpandLess, contentDescription = null)
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
                        horizontal = ProgressCardCollapsedHorizontalPadding,
                        vertical = ProgressCardCollapsedVerticalPadding,
                    ),
            ) {
                Text(
                    text = stringResource(R.string.simulation_route_progress_collapsed_label, percent, etaLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(Icons.Filled.ExpandMore, contentDescription = null)
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
