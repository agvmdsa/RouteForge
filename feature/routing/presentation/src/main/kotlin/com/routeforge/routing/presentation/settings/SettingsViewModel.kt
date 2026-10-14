package com.routeforge.routing.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.TourSection
import com.routeforge.coredomain.usecase.ObserveMapStyleUseCase
import com.routeforge.coredomain.usecase.SetMapStyleUseCase
import com.routeforge.routing.domain.usecase.GetStorageUsageSummaryUseCase
import com.routeforge.routing.domain.usecase.ObserveStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.SetStorageQuotaUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun TourSection.toNavigationEvent(): SettingsEvent =
    when (this) {
        TourSection.SIMULATION -> SettingsEvent.NavigateToSimulationTab
        TourSection.ROUTE_CREATION -> SettingsEvent.NavigateToRouteCreationTab
        TourSection.SAVED -> SettingsEvent.NavigateToSavedTab
    }

class SettingsViewModel(
    private val observeStorageQuota: ObserveStorageQuotaUseCase,
    private val setStorageQuota: SetStorageQuotaUseCase,
    private val getStorageUsageSummary: GetStorageUsageSummaryUseCase,
    private val observeMapStyle: ObserveMapStyleUseCase,
    private val setMapStyle: SetMapStyleUseCase,
    private val tourReplayCoordinator: TourReplayCoordinator,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState(usedBytes = getStorageUsageSummary()))
    val state = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeStorageQuota().collect { quotaBytes ->
                _state.update { it.copy(quotaBytes = quotaBytes) }
            }
        }
        viewModelScope.launch {
            observeMapStyle().collect { mapStyle ->
                _state.update { it.copy(mapStyle = mapStyle) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnQuotaSelected -> {
                setStorageQuota(action.quotaBytes)
                _state.update { it.copy(usedBytes = getStorageUsageSummary()) }
            }
            is SettingsAction.OnMapStyleSelected -> setMapStyle(action.mapStyle)
            is SettingsAction.OnRepeatTourClick -> repeatTour(action.section)
            SettingsAction.OnRepeatAllToursClick -> repeatAllTours()
        }
    }

    private fun repeatTour(section: TourSection) {
        viewModelScope.launch {
            _events.send(section.toNavigationEvent())
            tourReplayCoordinator.requestReplay(section)
        }
    }

    /** Navigates through all three sections in order (FR-008), waiting for each one's tour to
     *  finish (completed or skipped — [TourReplayCoordinator.notifyReplayFinished] fires either
     *  way) before advancing, so the user sees one section at a time rather than being dropped
     *  somewhere mid-sequence. */
    private fun repeatAllTours() {
        viewModelScope.launch {
            for (section in TourSection.entries) {
                _events.send(section.toNavigationEvent())
                tourReplayCoordinator.requestReplay(section)
                tourReplayCoordinator.replayCompletions.first { it == section }
            }
        }
    }
}
