package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.MapStyleStore
import com.routeforge.coredomain.model.MapStyle
import kotlinx.coroutines.flow.StateFlow

class ObserveMapStyleUseCase(
    private val store: MapStyleStore,
) {
    operator fun invoke(): StateFlow<MapStyle> = store.observeMapStyle()
}
