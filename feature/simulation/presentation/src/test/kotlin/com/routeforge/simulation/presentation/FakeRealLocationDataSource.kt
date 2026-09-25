package com.routeforge.simulation.presentation

import com.routeforge.coredomain.model.RealLocation
import com.routeforge.simulation.domain.RealLocationDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull

class FakeRealLocationDataSource : RealLocationDataSource {
    private val _location = MutableStateFlow<RealLocation?>(null)
    var permissionGranted = true

    var location: RealLocation?
        get() = _location.value
        set(value) {
            _location.value = value
        }

    override fun observeLocation(): Flow<RealLocation> =
        if (permissionGranted) _location.filterNotNull() else emptyFlow()
}
