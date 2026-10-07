package com.routeforge.routing.presentation.regioncoveragemap

sealed interface RegionCoverageMapAction {
    data class OnMapTapped(val latitude: Double, val longitude: Double) : RegionCoverageMapAction

    data object OnDismissTappedRegion : RegionCoverageMapAction
}
