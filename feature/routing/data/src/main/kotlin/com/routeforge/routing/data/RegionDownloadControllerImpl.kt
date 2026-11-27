package com.routeforge.routing.data

import com.routeforge.coredata.ForegroundServiceController
import com.routeforge.coredata.NoOpForegroundServiceController
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.RegionCatalog
import com.routeforge.coredomain.RegionUsageTracker
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Region
import com.routeforge.routing.domain.RegionDownloadController
import com.routeforge.routing.domain.RegionDownloadEvent
import com.routeforge.routing.domain.model.RegionDownloadState
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class RegionDownloadControllerImpl(
    private val downloadRegion: DownloadRegionUseCase,
    private val regionCatalog: RegionCatalog,
    private val regionUsageTracker: RegionUsageTracker,
    private val foregroundServiceController: ForegroundServiceController = NoOpForegroundServiceController,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : RegionDownloadController {
    private val _state = MutableStateFlow<RegionDownloadState?>(null)
    override val state: StateFlow<RegionDownloadState?> = _state.asStateFlow()

    // Buffered: a failure that happens while no screen listens must not hold the download open (state and notification stuck).
    private val _events = Channel<RegionDownloadEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    private var downloadJob: Job? = null

    override fun start(region: Region) {
        if (_state.value != null) return
        _state.value = RegionDownloadState(region, progress = 0f)
        foregroundServiceController.start()
        downloadJob =
            coroutineScope.launch {
                val result =
                    try {
                        downloadRegion(region.id) { fraction ->
                            _state.update { it?.copy(progress = fraction) }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.Error(DataError.Network.UNKNOWN)
                    }
                if (result is Result.Error) _events.trySend(RegionDownloadEvent.Failed(region.id))
                _state.value = null
                foregroundServiceController.stop()
            }
    }

    override fun cancel() {
        val current = _state.value ?: return
        downloadJob?.cancel()
        downloadJob = null
        regionCatalog.deleteRegion(current.region)
        regionUsageTracker.clear(current.region.id)
        _state.value = null
        foregroundServiceController.stop()
    }
}
