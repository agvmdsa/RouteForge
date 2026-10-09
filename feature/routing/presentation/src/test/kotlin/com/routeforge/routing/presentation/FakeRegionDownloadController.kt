package com.routeforge.routing.presentation

import com.routeforge.routing.domain.RegionDownloadController
import com.routeforge.routing.domain.RegionDownloadEvent
import com.routeforge.coredomain.model.Region
import com.routeforge.routing.domain.model.RegionDownloadState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class FakeRegionDownloadController : RegionDownloadController {
    private val _state = MutableStateFlow<RegionDownloadState?>(null)
    override val state: StateFlow<RegionDownloadState?> = _state

    private val _events = Channel<RegionDownloadEvent>()
    override val events: Flow<RegionDownloadEvent> = _events.receiveAsFlow()

    val startCalls = mutableListOf<Region>()
    var cancelCallCount = 0
        private set

    fun emit(state: RegionDownloadState?) {
        _state.value = state
    }

    suspend fun emitEvent(event: RegionDownloadEvent) {
        _events.send(event)
    }

    override fun start(region: Region) {
        if (_state.value != null) return
        startCalls.add(region)
        _state.value = RegionDownloadState(region, progress = 0f)
    }

    override fun cancel() {
        if (_state.value == null) return
        cancelCallCount++
        _state.value = null
    }
}
