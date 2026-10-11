package com.routeforge.mocklocationsetup.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.mocklocationsetup.domain.MockLocationSetupStateSource
import com.routeforge.mocklocationsetup.domain.model.SetupStep
import com.routeforge.mocklocationsetup.domain.model.SetupStepId
import com.routeforge.mocklocationsetup.domain.usecase.ObserveSetupStateUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MockLocationSetupVM"

class MockLocationSetupViewModel(
    private val observeSetupState: ObserveSetupStateUseCase,
    private val mockLocationSetupStateSource: MockLocationSetupStateSource,
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
            is MockLocationSetupAction.OnDeepLinkClick -> launchDeepLink(action.stepId)
        }
    }

    private fun launchDeepLink(stepId: SetupStepId) {
        val intent =
            when (stepId) {
                SetupStepId.ENABLE_DEVELOPER_OPTIONS -> mockLocationSetupStateSource.developerOptionsDeepLinkIntent()
                SetupStepId.SELECT_MOCK_LOCATION_APP -> mockLocationSetupStateSource.mockLocationAppDeepLinkIntent()
            }
        if (intent != null) {
            viewModelScope.launch {
                _events.send(MockLocationSetupEvent.LaunchDeepLink(intent))
            }
        } else {
            Log.w(TAG, "No deep link intent available for $stepId despite its button being shown")
        }
    }
}

private fun SetupStep.toUi(): SetupStepUi =
    SetupStepUi(
        id = id,
        title = title,
        explanation = explanation,
        instructions = instructions,
        canDeepLink = canDeepLink,
    )
