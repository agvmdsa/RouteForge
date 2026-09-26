package com.routeforge.simulation.domain

import kotlinx.coroutines.flow.Flow

interface RealLocationDataSource {
    fun observeLocation(): Flow<RealLocationUpdate>
}
