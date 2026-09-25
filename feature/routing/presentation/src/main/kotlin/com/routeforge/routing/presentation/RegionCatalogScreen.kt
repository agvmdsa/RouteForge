package com.routeforge.routing.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegionCatalogRoot(viewModel: RegionCatalogViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var downloadErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RegionCatalogEvent.DownloadFailed -> downloadErrorMessage = event.message
            }
        }
    }

    RegionCatalogScreen(
        state = state,
        downloadErrorMessage = downloadErrorMessage,
        onAction = viewModel::onAction,
    )
}

@Composable
fun RegionCatalogScreen(
    state: RegionCatalogState,
    onAction: (RegionCatalogAction) -> Unit,
    downloadErrorMessage: String? = null,
) {
    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                items(state.regions, key = { it.id }) { region ->
                    RegionRow(
                        region = region,
                        isNeeded = region.id in state.neededRegionIds,
                        downloadProgress = if (state.downloadingRegionId == region.id) state.downloadProgress else null,
                        onDownloadClick = { onAction(RegionCatalogAction.OnDownloadRegion(region.id)) },
                    )
                }
            }
            downloadErrorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            HorizontalDivider()
            Text(
                text = "© OpenStreetMap contributors",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

@Composable
private fun RegionRow(
    region: Region,
    isNeeded: Boolean,
    downloadProgress: Float?,
    onDownloadClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = region.displayName, style = MaterialTheme.typography.titleMedium)
                if (isNeeded) {
                    Text(
                        text = "Needed for your current route",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "${region.sizeDisplayText()} • ${region.status.toDisplayText()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (region.status == RegionStatus.NOT_DOWNLOADED || region.status == RegionStatus.PARTIALLY_DOWNLOADED) {
                Button(onClick = onDownloadClick) {
                    Text("Download")
                }
            }
        }
        if (downloadProgress != null) {
            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Region.sizeDisplayText(): String =
    if (status == RegionStatus.PARTIALLY_DOWNLOADED) {
        "${estimatedMissingBytes / 1_000_000} MB missing of ${approximateSizeBytes / 1_000_000} MB"
    } else {
        "${approximateSizeBytes / 1_000_000} MB"
    }

private fun RegionStatus.toDisplayText(): String =
    when (this) {
        RegionStatus.DOWNLOADED -> "Downloaded"
        RegionStatus.DOWNLOADING -> "Downloading…"
        RegionStatus.PARTIALLY_DOWNLOADED -> "Partially downloaded"
        RegionStatus.NOT_DOWNLOADED -> "Not downloaded"
    }

@Preview
@Composable
private fun RegionCatalogScreenPreview() {
    RouteForgeTheme {
        RegionCatalogScreen(
            state =
                RegionCatalogState(
                    regions =
                        listOf(
                            Region(
                                id = "downloaded",
                                displayName = "Berlin area",
                                minLatitude = 50.0,
                                minLongitude = 10.0,
                                maxLatitude = 55.0,
                                maxLongitude = 15.0,
                                tileIds = listOf("E10_N50.rd5"),
                                approximateSizeBytes = 45_000_000,
                                status = RegionStatus.DOWNLOADED,
                            ),
                            Region(
                                id = "benelux",
                                displayName = "Benelux area",
                                minLatitude = 50.0,
                                minLongitude = 5.0,
                                maxLatitude = 55.0,
                                maxLongitude = 10.0,
                                tileIds = listOf("E5_N50.rd5"),
                                approximateSizeBytes = 45_000_000,
                                status = RegionStatus.NOT_DOWNLOADED,
                            ),
                        ),
                ),
            onAction = {},
        )
    }
}
