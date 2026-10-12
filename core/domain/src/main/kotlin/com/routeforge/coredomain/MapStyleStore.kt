package com.routeforge.coredomain

import com.routeforge.coredomain.model.MapStyle
import kotlinx.coroutines.flow.StateFlow

/** Persists the user's chosen [MapStyle] — shared by every screen that renders
 *  [com.routeforge.designsystem.map.RouteForgeMap]. */
interface MapStyleStore {
    fun observeMapStyle(): StateFlow<MapStyle>

    fun setMapStyle(mapStyle: MapStyle)
}
