package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.RealLocationDataSource
import com.routeforge.simulation.domain.RealLocationUpdate
import kotlinx.coroutines.flow.Flow

class ObserveRealLocationUseCase(
    private val dataSource: RealLocationDataSource,
) {
    operator fun invoke(): Flow<RealLocationUpdate> = dataSource.observeLocation()
}
