package com.routeforge.coredomain.testing

import com.routeforge.coredomain.MapStyleStore
import com.routeforge.coredomain.model.MapStyle
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMapStyleStore(
    initialMapStyle: MapStyle = MapStyle.DEFAULT,
) : MapStyleStore {
    private val mapStyle = MutableStateFlow(initialMapStyle)

    override fun observeMapStyle() = mapStyle

    override fun setMapStyle(mapStyle: MapStyle) {
        this.mapStyle.value = mapStyle
    }
}
