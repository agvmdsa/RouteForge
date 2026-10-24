package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.simulation.domain.RealLocationFailure
import com.routeforge.simulation.domain.RealLocationUpdate
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val REAL_LOCATION_TIMEOUT_MILLIS = 30_000L

internal class RealLocationTracker(
    private val observeRealLocation: ObserveRealLocationUseCase,
    private val lastKnownRealLocationHolder: LastKnownRealLocationHolder,
) {
    private var realLocationObservationJob: Job? = null
    private var realLocationTimeoutJob: Job? = null

    internal fun mirrorLastKnownLocation(
        state: MutableStateFlow<SimulationState>,
        scope: CoroutineScope,
    ) {
        lastKnownRealLocationHolder.location
            .onEach { location -> state.update { it.copy(realLocation = location) } }
            .launchIn(scope)
    }

    internal fun startObservingRealLocationIfNotMocking(
        state: MutableStateFlow<SimulationState>,
        scope: CoroutineScope,
    ) {
        if (state.value.mockedSession == null) startObservingRealLocation(state, scope)
    }

    internal fun startObservingRealLocation(
        state: MutableStateFlow<SimulationState>,
        scope: CoroutineScope,
    ) {
        if (realLocationObservationJob?.isActive == true) return
        state.update { it.copy(isSearchingRealLocation = true, errorType = it.errorType.clearIfRealLocationRelated()) }
        realLocationTimeoutJob =
            scope.launch {
                delay(REAL_LOCATION_TIMEOUT_MILLIS)
                if (state.value.isSearchingRealLocation) {
                    state.update {
                        it.copy(isSearchingRealLocation = false, errorType = SimulationErrorType.REAL_LOCATION_TIMED_OUT)
                    }
                }
            }
        realLocationObservationJob =
            observeRealLocation()
                .onEach { update ->
                    realLocationTimeoutJob?.cancel()
                    when (update) {
                        is RealLocationUpdate.Fix -> {
                            lastKnownRealLocationHolder.set(update.location)
                            state.update {
                                it.copy(isSearchingRealLocation = false, errorType = it.errorType.clearIfRealLocationRelated())
                            }
                        }
                        is RealLocationUpdate.Unavailable -> {
                            state.update {
                                it.copy(isSearchingRealLocation = false, errorType = update.reason.toSimulationErrorType())
                            }
                        }
                    }
                }.launchIn(scope)
    }

    internal fun stopObservingRealLocation() {
        realLocationObservationJob?.cancel()
        realLocationObservationJob = null
        realLocationTimeoutJob?.cancel()
        realLocationTimeoutJob = null
    }
}

internal fun RealLocationFailure.toSimulationErrorType(): SimulationErrorType =
    when (this) {
        RealLocationFailure.ProviderDisabled -> SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED
        RealLocationFailure.PermissionDenied -> SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED
    }

/** Clears a real-location-related error once a search (re)starts or resolves — leaves any
 *  unrelated error (e.g. NOT_AUTHORIZED) untouched. */
internal fun SimulationErrorType?.clearIfRealLocationRelated(): SimulationErrorType? =
    when (this) {
        SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED,
        SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED,
        SimulationErrorType.REAL_LOCATION_TIMED_OUT,
        -> null
        else -> this
    }
