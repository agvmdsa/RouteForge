package com.routeforge.mocklocationsetup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.mocklocationsetup.domain.model.SetupStep
import com.routeforge.mocklocationsetup.domain.usecase.ObserveSetupStateUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MockLocationSetupViewModel(
    private val observeSetupState: ObserveSetupStateUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(MockLocationSetupState())
    val state = _state.asStateFlow()

    private val _events = Channel<MockLocationSetupEvent>()
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        val setupState = observeSetupState()
        _state.update {
            it.copy(
                pendingSteps = setupState.pendingSteps.map { step -> step.toUi() },
                isReady = setupState.isReady,
                isBlockedByPolicy = setupState.isBlockedByPolicy,
            )
        }
    }

    fun onAction(action: MockLocationSetupAction) {
        when (action) {
            is MockLocationSetupAction.OnDeepLinkClick ->
                viewModelScope.launch {
                    _events.send(MockLocationSetupEvent.LaunchDeepLink(action.intent))
                }
        }
    }
}

private fun SetupStep.toUi(): SetupStepUi =
    SetupStepUi(
        id = id,
        title = title,
        explanation = explanation,
        instructions = instructions,
        deepLinkIntent = deepLinkIntent,
    )
