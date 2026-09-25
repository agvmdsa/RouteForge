package com.routeforge.coredomain

import com.routeforge.coredomain.model.RealLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LastKnownRealLocationHolder {
    private val _location = MutableStateFlow<RealLocation?>(null)
    val location: StateFlow<RealLocation?> = _location.asStateFlow()

    fun set(location: RealLocation) {
        _location.value = location
    }
}
