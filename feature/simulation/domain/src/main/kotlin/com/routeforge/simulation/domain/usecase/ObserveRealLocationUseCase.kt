package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.RealLocationDataSource
import com.routeforge.simulation.domain.model.RealLocation
import kotlinx.coroutines.flow.Flow

class ObserveRealLocationUseCase(
    private val dataSource: RealLocationDataSource,
) {
    operator fun invoke(): Flow<RealLocation> = dataSource.observeLocation()
}
