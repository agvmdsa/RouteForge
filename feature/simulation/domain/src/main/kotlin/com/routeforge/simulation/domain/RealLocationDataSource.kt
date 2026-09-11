package com.routeforge.simulation.domain

import com.routeforge.simulation.domain.model.RealLocation
import kotlinx.coroutines.flow.Flow

interface RealLocationDataSource {
    fun observeLocation(): Flow<RealLocation>
}
