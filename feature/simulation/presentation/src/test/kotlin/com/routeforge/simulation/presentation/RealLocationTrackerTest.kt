package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.model.RealLocation
import com.routeforge.simulation.domain.RealLocationDataSource
import com.routeforge.simulation.domain.RealLocationFailure
import com.routeforge.simulation.domain.RealLocationUpdate
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RealLocationTrackerTest {
    private class ScriptedRealLocationDataSource : RealLocationDataSource {
        val updates = MutableSharedFlow<RealLocationUpdate>(extraBufferCapacity = 8)

        override fun observeLocation(): Flow<RealLocationUpdate> = updates
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val dataSource = ScriptedRealLocationDataSource()
    private val lastKnownRealLocationHolder = LastKnownRealLocationHolder()
    private val tracker =
        RealLocationTracker(
            observeRealLocation = ObserveRealLocationUseCase(dataSource),
            lastKnownRealLocationHolder = lastKnownRealLocationHolder,
        )

    private val fix = RealLocation(latitude = 1.0, longitude = 2.0)

    private val activeSession =
        SimulationSession(
            mode = SimulationMode.STATIONARY,
            status = SimulationStatus.RUNNING,
            latitude = 10.0,
            longitude = 20.0,
            bearingDegrees = 0f,
            speedMetersPerSecond = 0f,
            route = null,
            distanceTraveledMeters = 0.0,
        )

    private fun advanceVirtualTime(millis: Long) {
        dispatcher.scheduler.advanceTimeBy(millis)
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `starting marks the search as running and begins observing the data source`() {
        val state = MutableStateFlow(SimulationState())

        tracker.startObservingRealLocation(state, scope)

        assertTrue(state.value.isSearchingRealLocation)
        assertEquals(1, dataSource.updates.subscriptionCount.value)
    }

    @Test
    fun `starting clears an earlier real location error`() {
        val state = MutableStateFlow(SimulationState(errorType = SimulationErrorType.REAL_LOCATION_TIMED_OUT))

        tracker.startObservingRealLocation(state, scope)

        assertNull(state.value.errorType)
    }

    @Test
    fun `starting leaves an unrelated error untouched`() {
        val state = MutableStateFlow(SimulationState(errorType = SimulationErrorType.NOT_AUTHORIZED))

        tracker.startObservingRealLocation(state, scope)

        assertEquals(SimulationErrorType.NOT_AUTHORIZED, state.value.errorType)
    }

    @Test
    fun `a fix is stored in the last known holder and ends the search`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)

        dataSource.updates.tryEmit(RealLocationUpdate.Fix(fix))

        assertEquals(fix, lastKnownRealLocationHolder.location.value)
        assertFalse(state.value.isSearchingRealLocation)
    }

    @Test
    fun `a provider that is disabled gives its error and ends the search`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)

        dataSource.updates.tryEmit(RealLocationUpdate.Unavailable(RealLocationFailure.ProviderDisabled))

        assertEquals(SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED, state.value.errorType)
        assertFalse(state.value.isSearchingRealLocation)
    }

    @Test
    fun `a permission that is denied gives its error and ends the search`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)

        dataSource.updates.tryEmit(RealLocationUpdate.Unavailable(RealLocationFailure.PermissionDenied))

        assertEquals(SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED, state.value.errorType)
        assertFalse(state.value.isSearchingRealLocation)
    }

    @Test
    fun `no update within 30 seconds gives the timed out error and ends the search`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)

        advanceVirtualTime(30_001L)

        assertEquals(SimulationErrorType.REAL_LOCATION_TIMED_OUT, state.value.errorType)
        assertFalse(state.value.isSearchingRealLocation)
    }

    @Test
    fun `the search is still running just before the 30 seconds are over`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)

        advanceVirtualTime(29_999L)

        assertTrue(state.value.isSearchingRealLocation)
        assertNull(state.value.errorType)
    }

    @Test
    fun `a fix arriving in time cancels the timeout so no error appears later`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)
        dataSource.updates.tryEmit(RealLocationUpdate.Fix(fix))

        advanceVirtualTime(30_001L)

        assertNull(state.value.errorType)
    }

    @Test
    fun `a fix after the timeout clears the real location error`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)
        advanceVirtualTime(30_001L)

        dataSource.updates.tryEmit(RealLocationUpdate.Fix(fix))

        assertNull(state.value.errorType)
    }

    @Test
    fun `a fix does not clear an unrelated error`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)
        state.value = state.value.copy(errorType = SimulationErrorType.NOT_AUTHORIZED)

        dataSource.updates.tryEmit(RealLocationUpdate.Fix(fix))

        assertEquals(SimulationErrorType.NOT_AUTHORIZED, state.value.errorType)
    }

    @Test
    fun `a second start while the observation is active does nothing`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)
        dataSource.updates.tryEmit(RealLocationUpdate.Fix(fix))

        tracker.startObservingRealLocation(state, scope)

        assertFalse(state.value.isSearchingRealLocation)
        assertEquals(1, dataSource.updates.subscriptionCount.value)
    }

    @Test
    fun `stopping cancels the observation and the timeout`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)

        tracker.stopObservingRealLocation()
        advanceVirtualTime(30_001L)

        assertEquals(0, dataSource.updates.subscriptionCount.value)
        assertNull(state.value.errorType)
    }

    @Test
    fun `starting again after a stop observes the data source again`() {
        val state = MutableStateFlow(SimulationState())
        tracker.startObservingRealLocation(state, scope)
        tracker.stopObservingRealLocation()

        tracker.startObservingRealLocation(state, scope)

        assertEquals(1, dataSource.updates.subscriptionCount.value)
    }

    @Test
    fun `stopping when nothing was started does nothing`() {
        tracker.stopObservingRealLocation()

        assertEquals(0, dataSource.updates.subscriptionCount.value)
    }

    @Test
    fun `granting the location permission with no mocked session starts the observation`() {
        val state = MutableStateFlow(SimulationState())

        tracker.startObservingRealLocationIfNotMocking(state, scope)

        assertEquals(1, dataSource.updates.subscriptionCount.value)
    }

    @Test
    fun `granting the location permission while a mock is active does not search`() {
        val state = MutableStateFlow(SimulationState(mockedSession = activeSession))

        tracker.startObservingRealLocationIfNotMocking(state, scope)

        assertEquals(0, dataSource.updates.subscriptionCount.value)
        assertFalse(state.value.isSearchingRealLocation)
    }

    @Test
    fun `the last known location is mirrored into the state`() {
        val state = MutableStateFlow(SimulationState())
        tracker.mirrorLastKnownLocation(state, scope)

        lastKnownRealLocationHolder.set(fix)
        assertEquals(fix, state.value.realLocation)

        lastKnownRealLocationHolder.set(RealLocation(latitude = 3.0, longitude = 4.0))
        assertEquals(RealLocation(latitude = 3.0, longitude = 4.0), state.value.realLocation)
    }

    @Test
    fun `a provider failure maps to the provider disabled error`() {
        assertEquals(SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED, RealLocationFailure.ProviderDisabled.toSimulationErrorType())
    }

    @Test
    fun `a permission failure maps to the permission denied error`() {
        assertEquals(SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED, RealLocationFailure.PermissionDenied.toSimulationErrorType())
    }

    @Test
    fun `the three real location errors are cleared`() {
        assertNull(SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED.clearIfRealLocationRelated())
        assertNull(SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED.clearIfRealLocationRelated())
        assertNull(SimulationErrorType.REAL_LOCATION_TIMED_OUT.clearIfRealLocationRelated())
    }

    @Test
    fun `every other error and no error are left untouched`() {
        val realLocationErrors =
            setOf(
                SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED,
                SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED,
                SimulationErrorType.REAL_LOCATION_TIMED_OUT,
            )

        SimulationErrorType.entries.filterNot { it in realLocationErrors }.forEach { error ->
            assertEquals(error, error.clearIfRealLocationRelated())
        }
        assertNull(null.clearIfRealLocationRelated())
    }
}
