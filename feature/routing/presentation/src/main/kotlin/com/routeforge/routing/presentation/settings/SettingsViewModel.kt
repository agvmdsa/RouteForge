package com.routeforge.routing.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.usecase.ObserveMapStyleUseCase
import com.routeforge.coredomain.usecase.SetMapStyleUseCase
import com.routeforge.routing.domain.usecase.GetStorageUsageSummaryUseCase
import com.routeforge.routing.domain.usecase.ObserveStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.SetStorageQuotaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val observeStorageQuota: ObserveStorageQuotaUseCase,
    private val setStorageQuota: SetStorageQuotaUseCase,
    private val getStorageUsageSummary: GetStorageUsageSummaryUseCase,
    private val observeMapStyle: ObserveMapStyleUseCase,
    private val setMapStyle: SetMapStyleUseCase,
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
        }
    }
}
