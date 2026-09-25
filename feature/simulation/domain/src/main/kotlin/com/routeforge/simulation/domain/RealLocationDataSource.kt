package com.routeforge.simulation.domain

import com.routeforge.coredomain.model.RealLocation
import kotlinx.coroutines.flow.Flow

interface RealLocationDataSource {
    fun observeLocation(): Flow<RealLocation>
}
