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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private var repeatAllJob: Job? = null

    /** Asking for the replay is all Settings does — the app brings that section's tab forward and
     *  the section's own screen starts the tour. Settings itself is off screen by then. A single
     *  repeat also ends any "Repeat all" still waiting, so that sequence can't wake up later and
     *  carry on from the section the user just replayed on their own. */
    private fun repeatTour(section: TourSection) {
        repeatAllJob?.cancel()
        tourReplayCoordinator.requestReplay(section)
    }

    /** Replays every section in order (FR-008), waiting for each one's tour to finish
     *  (completed or skipped — [TourReplayCoordinator.notifyReplayFinished] fires either way)
     *  before asking for the next, so the user sees one section at a time rather than being dropped
     *  somewhere mid-sequence. Every section after the first is marked as continuing the sequence,
     *  so its screen can say so. This keeps running while the user is on another tab: the
     *  view model outlives Settings leaving the screen, and nothing here needs Settings composed. */
    private fun repeatAllTours() {
        repeatAllJob?.cancel()
        repeatAllJob =
            viewModelScope.launch {
                TourSection.entries.forEachIndexed { index, section ->
                    tourReplayCoordinator.requestReplay(section, continuesSequence = index > 0)
                    tourReplayCoordinator.replayCompletions.first { it == section }
                }
            }
    }
}
