package com.routeforge.simulation.presentation

import com.routeforge.coredomain.model.RealLocation
import com.routeforge.simulation.domain.RealLocationDataSource
import com.routeforge.simulation.domain.RealLocationFailure
import com.routeforge.simulation.domain.RealLocationUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeRealLocationDataSource : RealLocationDataSource {
    private val _location = MutableStateFlow<RealLocation?>(null)
    var permissionGranted = true

    var location: RealLocation?
        get() = _location.value
        set(value) {
            _location.value = value
        }

    override fun observeLocation(): Flow<RealLocationUpdate> =
        if (permissionGranted) {
            _location.filterNotNull().map { RealLocationUpdate.Fix(it) }
        } else {
            flowOf(RealLocationUpdate.Unavailable(RealLocationFailure.PermissionDenied))
        }
}
