package com.routeforge.routing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState(usedBytes = getStorageUsageSummary()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeStorageQuota().collect { quotaBytes ->
                _state.update { it.copy(quotaBytes = quotaBytes) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnQuotaSelected -> {
                setStorageQuota(action.quotaBytes)
                _state.update { it.copy(usedBytes = getStorageUsageSummary()) }
            }
        }
    }
}
