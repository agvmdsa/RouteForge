package com.routeforge.routing.data

import android.content.Context
import android.content.Intent
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.RegionDownloadController
import com.routeforge.routing.domain.RegionDownloadEvent
import com.routeforge.routing.domain.RegionUsageTracker
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionDownloadState
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
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

class RegionDownloadControllerImpl(
    private val downloadRegion: DownloadRegionUseCase,
    private val regionCatalog: RegionCatalog,
    private val regionUsageTracker: RegionUsageTracker,
    private val context: Context? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : RegionDownloadController {
    private val _state = MutableStateFlow<RegionDownloadState?>(null)
    override val state: StateFlow<RegionDownloadState?> = _state.asStateFlow()

    private val _events = Channel<RegionDownloadEvent>()
    override val events = _events.receiveAsFlow()

    private var downloadJob: Job? = null

    override fun start(region: Region) {
        if (_state.value != null) return
        _state.value = RegionDownloadState(region, progress = 0f)
        context?.startForegroundService(Intent(context, RegionDownloadForegroundService::class.java))
        downloadJob =
            coroutineScope.launch {
                val result =
                    downloadRegion(region.id) { fraction ->
                        _state.update { it?.copy(progress = fraction) }
                    }
                if (result is Result.Error) _events.send(RegionDownloadEvent.Failed(region.id))
                _state.value = null
                context?.stopService(Intent(context, RegionDownloadForegroundService::class.java))
            }
    }

    override fun cancel() {
        val current = _state.value ?: return
        downloadJob?.cancel()
        downloadJob = null
        regionCatalog.deleteRegion(current.region)
        regionUsageTracker.clear(current.region.id)
        _state.value = null
        context?.stopService(Intent(context, RegionDownloadForegroundService::class.java))
    }
}
