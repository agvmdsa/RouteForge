package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.MapStyleStore
import com.routeforge.coredomain.model.MapStyle

class SetMapStyleUseCase(
    private val store: MapStyleStore,
) {
    operator fun invoke(mapStyle: MapStyle) = store.setMapStyle(mapStyle)
}
