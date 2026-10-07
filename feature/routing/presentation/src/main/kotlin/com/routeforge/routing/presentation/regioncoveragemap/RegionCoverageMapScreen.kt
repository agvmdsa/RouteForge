package com.routeforge.routing.presentation.regioncoveragemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.map.RouteForgeMapRegionOverlay
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.presentation.R
import org.koin.androidx.compose.koinViewModel

private val OverlayLabelPadding = 16.dp
private val OverlayLabelShape = RoundedCornerShape(16.dp)
private val OverlayLabelContentPadding = 12.dp

@Composable
fun RegionCoverageMapRoot(viewModel: RegionCoverageMapViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegionCoverageMapScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun RegionCoverageMapScreen(
    state: RegionCoverageMapState,
    onAction: (RegionCoverageMapAction) -> Unit,
) {
    val cameraTarget =
        state.regions.firstOrNull()?.let { region ->
            (region.minLatitude + region.maxLatitude) / 2 to (region.minLongitude + region.maxLongitude) / 2
        }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            RouteForgeMap(
                markers = emptyList(),
                regions = state.regions.map { it.toOverlay() },
                cameraTarget = cameraTarget,
                onMapTap = { latitude, longitude -> onAction(RegionCoverageMapAction.OnMapTapped(latitude, longitude)) },
                modifier = Modifier.fillMaxSize(),
            )
            if (state.regions.isEmpty()) {
                OverlayLabel(text = stringResource(R.string.region_coverage_map_empty_message), alignment = Alignment.TopCenter)
            }
            state.tappedRegionLabel?.let { label ->
                OverlayLabel(
                    text = stringResource(R.string.region_coverage_map_tapped_region_label, label),
                    alignment = Alignment.BottomCenter,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.OverlayLabel(
    text: String,
    alignment: Alignment,
) {
    Surface(
        shape = OverlayLabelShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.align(alignment).padding(OverlayLabelPadding),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(OverlayLabelContentPadding),
        )
    }
}

private fun Region.toOverlay(): RouteForgeMapRegionOverlay =
    RouteForgeMapRegionOverlay(
        id = id,
        minLatitude = minLatitude,
        minLongitude = minLongitude,
        maxLatitude = maxLatitude,
        maxLongitude = maxLongitude,
        filled = status == RegionStatus.DOWNLOADED,
    )

@Preview
@Composable
private fun RegionCoverageMapScreenPreview() {
    RouteForgeTheme {
        RegionCoverageMapScreen(
            state =
                RegionCoverageMapState(
                    regions =
                        listOf(
                            Region(
                                id = "berlin",
                                displayName = "Berlin area",
                                minLatitude = 52.3,
                                minLongitude = 13.0,
                                maxLatitude = 52.7,
                                maxLongitude = 13.7,
                                tileIds = listOf("berlin.rd5"),
                                approximateSizeBytes = 45_000_000,
                                status = RegionStatus.DOWNLOADED,
                            ),
                            Region(
                                id = "benelux",
                                displayName = "Benelux area",
                                minLatitude = 50.7,
                                minLongitude = 3.3,
                                maxLatitude = 53.6,
                                maxLongitude = 7.2,
                                tileIds = listOf("benelux.rd5"),
                                approximateSizeBytes = 320_000_000,
                                status = RegionStatus.NOT_DOWNLOADED,
                            ),
                        ),
                    tappedRegionLabel = "berlin.rd5",
                ),
            onAction = {},
        )
    }
}
