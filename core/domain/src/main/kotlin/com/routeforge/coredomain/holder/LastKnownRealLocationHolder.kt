package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.RealLocation
import kotlinx.coroutines.flow.StateFlow

class LastKnownRealLocationHolder {
    private val holder = HandoffHolder<RealLocation?>(initial = null)
    val location: StateFlow<RealLocation?> = holder.value

    fun set(location: RealLocation) = holder.set(location)
}
